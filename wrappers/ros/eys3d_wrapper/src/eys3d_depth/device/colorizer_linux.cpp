#include "eys3d_depth/device/colorizer_linux.h"

#include "eys3d_depth/util/log.h"

// #define Z14_FAR  16383
#define Z14_NEAR 0
#define D11_FAR  0
#define D11_NEAR 2047
#define D8_FAR   0
#define D8_NEAR  255

EYS3D_DEPTH_BEGIN_NAMESPACE

void ColorizerLinux::Init(float z14_far,
    bool is_8bits,
    std::shared_ptr<CameraCalibration> calib_params) {
  ColorizerPrivate::Init(z14_far, is_8bits, calib_params);

  float m_zFar = z14_far;
  float m_zNear = Z14_NEAR;
  float m_d11Far = D11_FAR;
  float m_d11Near = D11_NEAR;
  float m_d8Far = D8_FAR;
  float m_d8Near = D8_NEAR;

  int m_nDepthColorMapMode = 4;  // for customer

  ColorPaletteGenerator::DmColorMode(
      m_ColorPalette, m_nDepthColorMapMode, m_d8Far, m_d8Near);
  ColorPaletteGenerator::DmGrayMode(
      m_GrayPalette, m_nDepthColorMapMode, m_d8Far, m_d8Near);

  ColorPaletteGenerator::DmColorMode11(
      m_ColorPaletteD11, m_nDepthColorMapMode, m_d11Far, m_d11Near);
  ColorPaletteGenerator::DmGrayMode11(
      m_GrayPaletteD11, m_nDepthColorMapMode, m_d11Far, m_d11Near);
  // SetBaseGrayPaletteD11(m_GrayPaletteD11);

  ColorPaletteGenerator::DmColorMode14(m_ColorPaletteZ14, m_zFar, m_zNear);
  ColorPaletteGenerator::DmGrayMode14(m_GrayPaletteZ14, m_zFar, m_zNear);
  // SetBaseGrayPaletteZ14(m_GrayPaletteZ14, zFar);
}

Image::pointer ColorizerLinux::Process(const Image::pointer& depth_buf,
    const DepthMode& depth_mode, bool from_user) {
  // Cache depth buf if from internal
  if (!from_user) CachedDepthBuffer(depth_buf);

  int depth_width = depth_buf->width();
  int depth_height = depth_buf->height();
  if (is_8bits_) {  // 8bits, usb2 (depth width is half, wanted need x2)
    depth_width = depth_width * 2;
  }

  if (depth_mode == DepthMode::DEPTH_RAW) {
    // ImageFormat::DEPTH_RAW
    if (is_8bits_) {  // 8bits, usb2
      auto depth_raw = ImageDepth::Create(ImageFormat::DEPTH_RAW,
          depth_width, depth_height, false);
      depth_raw->set_frame_id(depth_buf->frame_id());
      AdaptU2Raw(depth_buf->data(), depth_raw->data(),
          depth_width, depth_height);
      return depth_raw;
    } else {  // 14bits, usb3
      return depth_buf;
    }
  } else if (depth_mode == DepthMode::DEPTH_COLORFUL) {
    // ImageFormat::DEPTH_RGB
    auto depth_rgb = ImageDepth::Create(ImageFormat::DEPTH_RGB,
        depth_width, depth_height, false);
    depth_rgb->set_frame_id(depth_buf->frame_id());
    if (is_8bits_) {  // 8bits, usb2
      ColorPaletteGenerator::UpdateD8bitsDisplayImage_DIB24(
          m_ColorPalette, depth_buf->data(), depth_rgb->data(),
          depth_width, depth_height);
    } else {  // 14bits, usb3
      ColorPaletteGenerator::UpdateZ14DisplayImage_DIB24(
          m_ColorPaletteZ14, depth_buf->data(), depth_rgb->data(),
          depth_width, depth_height);
    }
    return depth_rgb;
  } else if (depth_mode == DepthMode::DEPTH_GRAY) {
    // ImageFormat::DEPTH_GRAY_24
    auto depth_gray = ImageDepth::Create(
        ImageFormat::DEPTH_GRAY_24,
        depth_width, depth_height, false);
    depth_gray->set_frame_id(depth_buf->frame_id());
    if (is_8bits_) {  // 8bits, usb2
      ColorPaletteGenerator::UpdateD8bitsDisplayImage_DIB24(
          m_GrayPalette, depth_buf->data(), depth_gray->data(),
          depth_width, depth_height);
    } else {  // 14bits, usb3
      ColorPaletteGenerator::UpdateZ14DisplayImage_DIB24(
          m_GrayPaletteZ14, depth_buf->data(), depth_gray->data(),
          depth_width, depth_height);
    }
    return depth_gray;
  } else {
    LOGE("Colorizer unaccepted depth mode: %s", depth_mode);
    return nullptr;
  }
}
EYS3D_DEPTH_END_NAMESPACE
