#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QDateTime>
#include<QtNetwork>
#include <QAxObject>
#include"handleudp.h"

namespace Ui {
class MainWindow;
}

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = 0);
    qint64 getTime();
    ~MainWindow();

private:
    Ui::MainWindow *ui;
    bool createConnection();

    QAxObject * document;
    QAxObject * parentWindow;

    QHostAddress localAddr;     //本地端IP
    quint16 localPort;      //本地端端口
    QHostAddress receiveAddr; //接收端IP
    quint16 receivePort;      //接收端端口

    bool isUdpStarted = false;
    bool isThreadStarted = false;

    handleUdp *handleudp = nullptr;   //创建handleudp对象



private slots:

    void on_btn_start_clicked();
    void onUdpStopButtonClicked();
    void onUdpAppendMessage(const QString &from, const QJsonObject &message);


signals:
    void newMessage(const QJsonObject &message);        //新的信息
    void newLogInfo(const QString &logInfo);            //发送调试信息
};

#endif // MAINWINDOW_H
