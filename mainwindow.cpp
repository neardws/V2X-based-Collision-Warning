#include "mainwindow.h"
#include "ui_mainwindow.h"
#include "mybridgeofjs.h"

#include <QPushButton>
#include <QLineEdit>
#include <QDoubleSpinBox>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QGridLayout>
#include <QDir>
#include <QDebug>
#include <QJsonDocument>
#include <QJsonObject>
#include <QStringList>
#include <QJsonArray>
#include<QString>


MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    resize(800,1024);

    //加载百度地图界面相关
    m_pWebView = new QWebEngineView(this);
        // 关闭自动代理
    QNetworkProxyFactory::setUseSystemConfiguration(false);
        //创建通道对象用于与JS交互
    m_pWebchannel = new QWebChannel(this);
    MybridgeofJS *mybride = new MybridgeofJS();
    m_pWebchannel->registerObject("bridge_name",(QObject*)mybride);
    m_pWebView->page()->setWebChannel(m_pWebchannel);
        //载入百度地图地址
   // QDir temDir("../V2X-based-Collision-Warning/trace.html");
//    QString absDir = temDir.absolutePath();
//    QString filePath = "file:///" + absDir;
//    qDebug()<<filePath;
    m_pWebView->page()->load(QUrl::fromLocalFile("D:/resource/trace.html"));
    //m_pWebView->page()->load(QUrl(filePath));
    ui->vLayout_map->addWidget(m_pWebView);

    //初始化udp
   // mUDPSocket = new QUdpSocket(this);
    myudp=new MyUDP;

    connect(ui->btn_trace,SIGNAL(clicked()),this,SLOT(on_btnTrace_clicked()));
    connect(ui->btn_clear,SIGNAL(clicked()),this,SLOT(on_btnClear_clicked()));



}

MainWindow::~MainWindow()
{
    delete ui;
}

void MainWindow::slotDealMsg()
{
    while(mUDPSocket->hasPendingDatagrams())
    {
        QByteArray datagram;
        QHostAddress sender;
        quint16 senderPort;
        datagram.resize(mUDPSocket->pendingDatagramSize());
        mUDPSocket->readDatagram(datagram.data(),datagram.size(),&sender,&senderPort);

        qDebug() <<"datagram "<<datagram ;
        ui->plainTextEdit->appendPlainText("from "+sender.toString()+":"+QString(datagram)+"\n");

        paseJSon(datagram);

    }
}

void MainWindow::on_pushButton_clicked()
{
    //mUDPSocket = new QUdpSocket(this);
    //udp
    //绑定
    udpListenPort = ui->lineEdit_Port->text().toInt();
    localAddr.setAddress(ui->lineEdit_IP->text());
    qDebug()<<localAddr<<"+"<<udpListenPort;
    myudp->bindPort(localAddr,udpListenPort);
    connect(myudp, SIGNAL(newMessage(QString, QJsonObject)), this, SLOT(onUdpAppendMessage(QString, QJsonObject)));
    //mUDPSocket->bind(mServerPort);

    //qDebug() << mServerPort;
   // connect(mUDPSocket, &QUdpSocket::readyRead, this, &MainWindow::slotDealMsg,Qt::UniqueConnection);
//    mAllCarInfo.push_back(carinfo);
ui->pushButton->setEnabled(false);
}

void MainWindow::onUdpAppendMessage(const QString &from, const QJsonObject &message){

    //将jsonObeject转换成可以被输出到日志框的格式
    QJsonDocument document;
    document.setObject(message);
    QByteArray message_array = document.toJson(QJsonDocument::Compact);
    QString simpjson_str(message_array);




        //解析json格式的各项数据信息
        QString         id               = message.find("device_id").value().toString();
        double   timeStamp        = message.find("current_time").value().toDouble();
        double       direction        = message.find("hea").value().toDouble();
        double      lat              = message.find("lat").value().toDouble();
        double      lon              = message.find("lon").value().toDouble();
        double       speed            = message.find("spd").value().toDouble();


        //打印json的完整信息在日志框中
        ui->plainTextEdit->appendPlainText(QString::fromLocal8Bit("接收id=")+id+QString::fromLocal8Bit("传来的信息:")+simpjson_str);


        //展示车辆1的信息
        if(id=="1")
        {

            //展示信息
            ui->lineEdit_car1_id->setText(id);
            ui->lineEdit_car1_lon->setText(QString::number(lon,10,6));
            ui->lineEdit_car1_lat->setText(QString::number(lat,10,6));
            ui->lineEdit_car1_vel->setText(QString::number(speed,10,6));
            ui->lineEdit_car1_hea->setText(QString::number(direction,10,6));
            ui->lineEdit_car1_timestamp->setText(QString::number(timeStamp));

            //绘制车辆1轨迹

            //第一次全局清除
           if(initial)
           {
               //   清除上一次的点
               QString str = "send()";
               m_pWebView->page()->runJavaScript(str);

               //把要调用的JS命令当做QString传递给网页
               QString cmd = QString("add_car_one_marker(%0,%1,%2)").arg(QString::number(lon,'f', 6)).arg(QString::number(lat,'f', 6)).arg(id);
               qDebug() << cmd;
               //实现QT通过C++调用JS函数
               m_pWebView->page()->runJavaScript(cmd);
               initial=0;
           }
           else
           {
               //把要调用的JS命令当做QString传递给网页
               QString cmd = QString("add_car_one_marker(%0,%1,%2)").arg(QString::number(lon,'f', 6)).arg(QString::number(lat,'f', 6)).arg(id);
               qDebug() << cmd;
               //实现QT通过C++调用JS函数
               m_pWebView->page()->runJavaScript(cmd);

           }
        }
        //展示车辆2的信息
        else if(id=="2")
        {
            //展示信息
            ui->lineEdit_car2_id->setText(id);
            ui->lineEdit_car2_lon->setText(QString::number(lon,10,6));
            ui->lineEdit_car2_lat->setText(QString::number(lat,10,6));
            ui->lineEdit_car2_vel->setText(QString::number(speed,10,6));
            ui->lineEdit_car2_hea->setText(QString::number(direction,10,6));
            ui->lineEdit_car2_timestamp->setText(QString::number(timeStamp));

            //绘制车辆2轨迹
            //第一次全局清除
            if(initial)
            {
                //   清除上一次的点
                QString str = "send()";
                m_pWebView->page()->runJavaScript(str);

                //把要调用的JS命令当做QString传递给网页
                QString cmd = QString("add_car_two_marker(%0,%1,%2)").arg(QString::number(lon,'f', 6)).arg(QString::number(lat,'f', 6)).arg(id);
                qDebug() << cmd;
                //实现QT通过C++调用JS函数
                m_pWebView->page()->runJavaScript(cmd);
                initial=0;
            }
            else
            {
                //把要调用的JS命令当做QString传递给网页
                QString cmd = QString("add_car_two_marker(%0,%1,%2)").arg(QString::number(lon,'f', 6)).arg(QString::number(lat,'f', 6)).arg(id);
                qDebug() << cmd;
                //实现QT通过C++调用JS函数
                m_pWebView->page()->runJavaScript(cmd);

            }
        }



       emit newMessage(message);
}

void MainWindow::on_btnTrace_clicked()
{
    QString cmd = QString("add_car_one_trace();add_car_two_trace();");
     qDebug() << cmd;
     m_pWebView->page()->runJavaScript(cmd);//传给javascript
}

void MainWindow::on_btnClear_clicked()
{
    QString cmd = QString("remove_trace()");
     qDebug() << cmd;
     m_pWebView->page()->runJavaScript(cmd);//传给javascript
}


void MainWindow::paseJSon(QByteArray all)
{

    QJsonDocument doc = QJsonDocument::fromJson(all);

     QJsonArray array = doc.array();

     //bool setMarker = true;
     if(!array.empty())
     {
         foreach(auto var , array)
         {
           CarInfoStruct info;

           QJsonObject obj = var.toObject();// var.Object()
           QStringList keys = obj.keys();//得到所有key
           for (int i = 0; i < keys.size(); i++)
           {
               QString key = keys.at(i);
               QJsonValue value = obj.value(key);
               if(0==key.compare("lon"))
               {
                   info.lon = value.toDouble();
                   QString lon = QString::number(info.lon,'f',10);
                   qDebug()<<lon;
               }
               else if(0==key.compare("lat")){
                   info.lat = value.toDouble();
               }
               else if(0==key.compare("speed")){
                   info.speed = value.toDouble();
               }
               else if(0==key.compare("acc")){
                   info.acc = value.toDouble();
               }

           }



         QString winfo = QString("speed:%0  acc:%1 ").arg(info.speed).arg(info.acc);
         qDebug() << winfo;

        //把要调用的JS命令当做QString传递给网页
        QString cmd = QString("addmarkerEx(%0,%1,'%2')").arg(info.lon).arg(info.lat).arg(winfo);
        qDebug() << cmd;

        //实现QT通过C++调用JS函数
        m_pWebView->page()->runJavaScript(cmd);
     }
    }

}
