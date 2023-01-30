#include "mainwindow.h"
#include <QApplication>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    MainWindow w;
    w.setWindowTitle("C-V2X车辆碰撞预警系统");
    w.show();

    return a.exec();
}
