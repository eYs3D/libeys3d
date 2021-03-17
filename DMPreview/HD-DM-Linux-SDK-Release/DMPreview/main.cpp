#include "mainwindow.h"
#include <QApplication>
#include <QMainWindow>
#include "eSPDI_version.h"
#include "CPointCloudViewerWidget.h"

#include <csignal>
void signalHandler(int signum)
{
    exit(signum);
}

int main(int argc, char *argv[])
{
    signal(SIGINT, signalHandler);

    QApplication app(argc, argv);

    MainWindow mainWindow;
    //mainWindow.setWindowIcon(QIcon(":/image/eys3d.png"));
    QString title = "DMPreview ";
    title += ETRONDI_VERSION;
    mainWindow.setWindowTitle(title);
    mainWindow.show();

    return app.exec();
}
