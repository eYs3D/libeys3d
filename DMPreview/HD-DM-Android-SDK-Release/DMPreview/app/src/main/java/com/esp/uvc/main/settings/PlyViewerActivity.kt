package com.esp.uvc.main.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.esp.uvc.BaseFragment
import com.esp.uvc.R
import com.esp.uvc.application.AndroidApplication
import com.esp.uvc.main.common.ProgressDialogFragment
import com.esp.uvc.ply.PlyParser
import com.esp.uvc.ply.viewer.GLRenderer
import com.esp.uvc.utils.roUI
import com.esp.uvc.views.PlyFileAdapter
import kotlinx.android.synthetic.main.activity_ply_viewer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

private const val REQUEST_CODE = 100

@SuppressLint("SdCardPath")
private const val PLY_PATH = "/sdcard/eYs3D/"

class PlyViewerActivity : AppCompatActivity(), PlyFileAdapter.OnClickListener {

    private var mFiles: List<File>? = null

    private val mGLRenderer by lazy { GLRenderer() }

    private val mAdapter by lazy {
        val adapter = PlyFileAdapter()
        adapter.setOnClickListener(this)
        adapter
    }

    private val mProgressDialog by lazy { ProgressDialogFragment() }

    private var mToast: Toast? = null

    companion object {
        fun startInstance(context: Context) {
            context.startActivity(Intent(context, PlyViewerActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        BaseFragment.applyScreenRotation(this)
        super.onCreate(savedInstanceState)
        title = getString(R.string.ply_viewer_title)
        setContentView(R.layout.activity_ply_viewer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (setUpGlEs()) {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@PlyViewerActivity)
                adapter = mAdapter
            }
            if (checkPermissions()) {
                mFiles = getPlyFileList()
                if (mFiles == null || mFiles!!.isEmpty()) {
                    toast(getString(R.string.ply_viewer_file_not_found_error, PLY_PATH))
                } else {
                    mAdapter.setFiles(mFiles!!.map { it.name })
                }
            } else {
                requestPermissions()
            }
        } else {
            toast(getString(R.string.ply_viewer_requires_gl_es_version_error))
        }
    }

    override fun onPause() {
        super.onPause()
        if (ply_gl_surfaceView.visibility == View.VISIBLE) {
            ply_gl_surfaceView.visibility = View.GONE
            ply_gl_surfaceView.onPause()
        }
    }

    override fun onBackPressed() {
        if (ply_gl_surfaceView.visibility == View.VISIBLE) {
            ply_gl_surfaceView.visibility = View.GONE
            ply_gl_surfaceView.onPause()
            bt_reset.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    mFiles = getPlyFileList()
                    if (mFiles == null || mFiles!!.isEmpty()) {
                        toast(getString(R.string.ply_viewer_file_not_found_error, PLY_PATH))
                    } else {
                        mAdapter.setFiles(mFiles!!.map { it.name })
                    }
                }
            }
        }
    }

    private fun setUpGlEs(): Boolean {
        val supportGlEs = AndroidApplication.eglMajorVersion
        when {
            supportGlEs >= 0x30000 -> ply_gl_surfaceView.setEGLContextClientVersion(3)
            supportGlEs >= 0x20000 -> ply_gl_surfaceView.setEGLContextClientVersion(2)
            else -> return false
        }
        ply_gl_surfaceView.setRenderer(mGLRenderer)
        return true
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_CODE
        )
    }

    private fun getPlyFileList(): List<File>? {
        return File(PLY_PATH).listFiles()?.filter { it.name.substringAfterLast(".") == "ply" }
    }

    private fun toast(text: String) = roUI {
        if (mToast != null) {
            mToast!!.cancel()
        }
        mToast = Toast.makeText(this, text, Toast.LENGTH_LONG)
        mToast!!.show()
    }

    fun onResetClick(view: View) {
        mGLRenderer.reset()
    }

    override fun onClick(file: String) {
        mProgressDialog.setProgressInfo(file)
        mProgressDialog.show(supportFragmentManager, "progressDialog")
        GlobalScope.launch(Dispatchers.IO) {
            val plyParser = PlyParser(mFiles!!.find { it.name == file }!!.inputStream())
            if (plyParser.parsePly()) {
                GlobalScope.launch(Dispatchers.Main) {
                    mProgressDialog.dismiss()
                    mGLRenderer.setData(
                        plyParser.getVertices()!!,
                        plyParser.getColors()!!,
                        plyParser.getVertexCount()
                    )
                    ply_gl_surfaceView.visibility = View.VISIBLE
                    ply_gl_surfaceView.onResume()
                    bt_reset.visibility = View.VISIBLE
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    mProgressDialog.dismiss()
                    toast(getString(R.string.ply_viewer_parse_error))
                }
            }
        }
    }
}