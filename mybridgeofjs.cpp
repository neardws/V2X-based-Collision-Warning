#include "mybridgeofjs.h"
#include <QMessageBox>
#include <QJSEngine>
#include <QDebug>

#pragma execution_character_set("utf-8")

MybridgeofJS::MybridgeofJS(QObject *parent) : QObject(parent)
{

}

//void MybridgeofJS::getJSMsg()
//{
//    emit sigJSMsg("hello, world!");
//}

void MybridgeofJS::getCoordinate(QString lon, QString lat)
{
    QString strValue;
    strValue= QString("lon= %1,lat= %2").arg(lon).arg(lat);
    QMessageBox::information(nullptr, "Qt提示", strValue, QMessageBox::Ok) ;
}
