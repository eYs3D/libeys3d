#ifndef CVIDEODEVICEDEPTHACCURACYWIDGET_H
#define CVIDEODEVICEDEPTHACCURACYWIDGET_H

#include <QWidget>
#include "CEtronUIView.h"

namespace Ui {
class CVideoDeviceDepthAccuracyWidget;
}

class CDepthAccuracyController;
class CVideoDeviceDepthAccuracyWidget : public QWidget,
                                        public CEtronUIView
{
    Q_OBJECT
public:
    explicit CVideoDeviceDepthAccuracyWidget(CDepthAccuracyController *pCameraPropertyController,
                                             QWidget *parent = nullptr);
    ~CVideoDeviceDepthAccuracyWidget();

    virtual void paintEvent(QPaintEvent *event);
    virtual void showEvent(QShowEvent *event);
    virtual void hideEvent(QHideEvent *event);
    virtual void UpdateSelf();

private slots:
    void on_comboBox_depth_accuracy_stream_currentTextChanged(const QString &text);

    void on_comboBox_depth_accuracy_roi_currentTextChanged(const QString &text);

    void on_checkBox_depth_accuracy_ground_truth_stateChanged(int state);

    void on_doubleSpinBox_depth_accuracy_ground_truth_vaule_valueChanged(double dblValue);

    void on_horizontalSlider_pixel_unit_value_valueChanged(int value);

    void on_pushButton_clicked();

private:
    void UpdateAccuracyList();
    void UpdateAccuracyRegion();
    void UpdateGroundTruth();
    void UpdateValue();

    void UpdateAdjustingFocalLength();
    void UpdatePixelUnit();
    void UpdateFocalLength();

private:
    CDepthAccuracyController *m_pDepthAccuracyController;
    Ui::CVideoDeviceDepthAccuracyWidget *ui;
};

#endif // CVIDEODEVICEDEPTHACCURACYWIDGET_H
