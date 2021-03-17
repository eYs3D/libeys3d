package com.esp.uvc.di

import com.esp.android.usb.camera.core.USBMonitor
import com.esp.uvc.main.settings.*
import com.esp.uvc.main.settings.module_sync.IModuleSync
import com.esp.uvc.main.settings.module_sync.MModuleSync
import com.esp.uvc.main.settings.module_sync.PModuleSync
import com.esp.uvc.main.settings.sensor.ISensor
import com.esp.uvc.main.settings.sensor.MSensorSettings
import com.esp.uvc.main.settings.sensor.PSensorSettings
import com.esp.uvc.ply.IMUMesh
import com.esp.uvc.ply.Mesh
import com.esp.uvc.roi_size.RoiSizeProvider
import com.esp.uvc.roi_size.RoiSizeSharedPrefsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * appModule used within the Koin dependency framework
 * It represents the modules that will be injected via koins mechanisms
 */
val appModule = module {
    factory { SoftwareVersionPresenter() as SoftwareVersionContract.Presenter }
    factory { FirmwareVersionPresenter(androidContext()) as FirmwareVersionContract.Presenter }
    factory { FirmwareRegisterPresenter(androidContext()) as FirmwareRegisterContract.Presenter }
    factory { PreviewImagePresenter(androidContext()) as PreviewImageContract.Presenter }
    factory { FirmwareTablePresenter(androidContext()) as FirmwareTableContract.Presenter }
    factory { RoiSizeSharedPrefsProvider(androidContext()) as RoiSizeProvider }
    // All use di is better ?!
    factory { (view: ISensor.View) -> PSensorSettings(view) as ISensor.Presenter }
    factory { (presenter: ISensor.Presenter) -> MSensorSettings(presenter) as ISensor.Model  }
    factory { (view: IModuleSync.View, usbMonitor: USBMonitor) -> PModuleSync(view, usbMonitor) as IModuleSync.Presenter }
    factory { (presenter: IModuleSync.Presenter, usbMonitor: USBMonitor) -> MModuleSync(presenter, usbMonitor) as IModuleSync.Model  }
    factory { Mesh() }
    factory { IMUMesh() }
}