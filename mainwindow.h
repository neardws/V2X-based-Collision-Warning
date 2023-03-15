#ifndef MAINWINDOW_H
#define MAINWINDOW_H
#include "myudp.h"
#include <QMainWindow>
#include <QWebEngineView>
#include <QNetworkProxyFactory>
#include <QWebEnginePage>
#include <QWebChannel>
#include <QHostAddress>
#include <QUdpSocket>
namespace Ui {
class MainWindow;
}

struct CarInfoStruct{
    CarInfoStruct()
    {
        timestamp =0;
        id =0;
        lon =0;
        lat =0;
        speed =0;
        acc =0;
        dir =0;
    }
    long timestamp;
    int id;
    double lon;
    double lat;
    double speed;
    double acc;
    int dir;
};


class MainWindow : public QMainWindow
{

    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    /****************************
     *
     * UDP通信模块槽函数
     * on_sendButton_clicked   发送按钮单击事件的槽函数
     * onUdpSendMessage        使用UDP发送消息的槽函数
     * onUdpAppendMessage      UDP接收到消息的槽函数
     *
     ******************************/
    void on_pushButton_clicked();
    //void onUdpStopButtonClicked();
    //void onUdpSendMessage();
    //void onSendMessage(const QJsonObject &result);
    void onUdpAppendMessage(const QString &from, const QJsonObject &message);
    //void on_pushButton_Stop_clicked();
public slots:
    void  slotDealMsg();
private:
    void paseJSon(QByteArray all);


private:
    Ui::MainWindow *ui;
private:
    QWebEngineView* m_pWebView;
    QWebChannel* m_pWebchannel;
    QList<CarInfoStruct> mAllCarInfo;

    QString mJSONStr;

    QUdpSocket *mUDPSocket ;
    QUdpSocket *m_ptrUdpClient;

    //int initial=0;
    bool initial=1;


    QString mServerIP;
    int mServerPort;

    QHostAddress localAddr;     //本地IP地址
    quint16 udpListenPort  = 9000;      //本地接收端的监听端口
    QHostAddress udpTargetAddr; //目标接收端的IP地址
    quint16 udpTargetPort;      //目标接收端的监听端口

    MyUDP *myudp = nullptr;   //MyUDP 对象

signals:
    void newMessage(const QJsonObject &message);            //有新的信息，添加到队列中
};

#endif // MAINWINDOW_H
