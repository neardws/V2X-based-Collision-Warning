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
#include<QPalette>

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
   // m_pWebView->page()->load(QUrl::fromLocalFile("D:/resource/trace.html"));

    //导入离线地图
    m_pWebView->page()->load(QUrl::fromLocalFile("D:/resource/mymaphtml/bmap-demo/001/baidumap_offline_unconverted.html"));

    //m_pWebView->page()->load(QUrl(filePath));
    ui->vLayout_map->addWidget(m_pWebView);

    //初始化udp
   // mUDPSocket = new QUdpSocket(this);
    myudp=new MyUDP;
    //启动子线程
    computeThread->start();


    connect(ui->btn_trace,SIGNAL(clicked()),this,SLOT(on_btnTrace_clicked()));
   // connect(ui->btn_clear,SIGNAL(clicked()),this,SLOT(on_btnClear_clicked()));
    connect(ui->pushButton_cancel,SIGNAL(clicked()),this,SLOT(on_pushButtonCancel_clicked()));


}

MainWindow::~MainWindow()
{
    delete ui;
    computeThread->quit();
    computeThread->wait();

}


void MainWindow::on_pushButton_clicked()
{

    //mUDPSocket = new QUdpSocket(this);

    //udp设置
    //设置端口号为50501
    udpListenPort=50501;
    //设置ip地址
    localAddr.setAddress("192.168.20.224");
    qDebug()<<localAddr<<"+"<<udpListenPort;
    //绑定udp连接
    myudp->bindPort(localAddr,udpListenPort);
    //接收udp传来的信息的连接
    connect(myudp, SIGNAL(newMessage(QString, QJsonObject)), this, SLOT(onUdpAppendMessage(QString, QJsonObject)));



   //注册ncs{“tag”:2001, “data” : { “ip” : “192.168.20.224”, “port” : 50501} }
     QJsonObject sign;
     sign.insert("tag",2001);
     QJsonObject data;
     data.insert("ip","192.168.20.224");
     data.insert("port",50501);
     sign.insert("data",data);
     requestAddr.setAddress("192.168.20.199");
     myudp->sendMessage(requestAddr,50500,sign);

    ui->pushButton->setEnabled(false);
    ui->pushButton_cancel->setEnabled(true);
   // ui->lineEdit_showWarning->setText("NO");

    //一旦发出newMessage的信号，触发子线程中接收数据的槽函数
    connect(this, SIGNAL(newMessage(QJsonObject,long long)), computeThread, SLOT(recvJsonMessage(QJsonObject,long long)));
    connect(computeThread,SIGNAL(sendDistance(double)),this,SLOT(showDistance(double)));
    connect(computeThread,SIGNAL(sendWarning(QJsonObject)),this,SLOT(sendCollisionWarning(QJsonObject)));
    //connect(computeThread,SIGNAL(sendWarning(QJsonObject)),this,SLOT(showWarning(QJsonObject)));
    connect(computeThread,SIGNAL(sendDebug(QString)),this,SLOT(appendText(QString)));
}

void MainWindow::on_pushButtonCancel_clicked()
{
    myudp->unbindPort();
    ui->pushButton_cancel->setEnabled(false);
    ui->pushButton->setEnabled(true);
}

void MainWindow::onUdpAppendMessage(const QString &from, const QJsonObject &message){
 long long timeStamp =getTimeStamp();
    //将jsonObeject转换成可以被输出到日志框的格式
    QJsonDocument document;
    document.setObject(message);
    QByteArray message_array = document.toJson(QJsonDocument::Compact);
    QString simpjson_str(message_array);
    qDebug()<<simpjson_str;

    //解析接口号对应不同的输出和操作
    int tag=message.find("tag").value().toInt();
    //响应信息
    if(tag==1002)
    {
        //打印json的完整信息在日志框中
        ui->plainTextEdit->appendPlainText(QString::fromLocal8Bit("接收接口号=")+QString::number(tag)+QString::fromLocal8Bit("传来的信息:")+simpjson_str);
    }
    //注册返回信息
    else if(tag==2002)
    {
         ui->plainTextEdit->appendPlainText(QString::fromLocal8Bit("接收接口号=")+QString::number(tag)+QString::fromLocal8Bit("传来的注册信息:")+simpjson_str);
         //保存唯一id
          QString unique = message.find("unique").value().toString();
    }
    //接收上报信息
    else if(tag==2102)
    {
        emit newMessage(message,timeStamp);
       ui->plainTextEdit->appendPlainText(QString::fromLocal8Bit("接收接口号=")+QString::number(tag)+QString::fromLocal8Bit("传来的信息:")+simpjson_str);
        //解析json格式的各项数据信息
        QJsonValue data=message.find("data").value();
        QJsonArray dataArr=message.find("data").value().toArray();
        //QJsonObject dataobj=data.toObject();
        for(int i=0; i<dataArr.size();i++){
            QJsonValue datachild = dataArr.at(i);
            QJsonObject dataobj=datachild.toObject();
            QString         id               = dataobj.find("device_id").value().toString();
           // QString   timeStamp        = message.find("current_time").value().toString();
            double       direction        = dataobj.find("hea").value().toDouble();
            double      lat              = dataobj.find("lat").value().toDouble();
            double      lon              = dataobj.find("lon").value().toDouble();
            double       speed            = dataobj.find("spd").value().toDouble();

            double wgs84_lon = lon;
            double wgs84_lat = lat;
            double gcj02_lon = wgs84togcj02(wgs84_lon,wgs84_lat).longitude;
            double gcj02_lat = wgs84togcj02(wgs84_lon,wgs84_lat).latitude;
            double bd09_lon = gcj02tobd09(gcj02_lon,gcj02_lat).longitude;
            double bd09_lat = gcj02tobd09(gcj02_lon,gcj02_lat).latitude;

           //ui->plainTextEdit->appendPlainText("lon="+QString::number(bd09_lon,10,6)+"，lat="+QString::number(bd09_lat,10,6));

            //展示车辆1(obu:74)的信息
            if(id=="21030096")
            {

                //展示信息
                ui->lineEdit_car1_id->setText(id);
                ui->lineEdit_car1_lon->setText(QString::number(lon,10,6));
                ui->lineEdit_car1_lat->setText(QString::number(lat,10,6));
                ui->lineEdit_car1_vel->setText(QString::number(speed,10,6));
                ui->lineEdit_car1_hea->setText(QString::number(direction,10,2));
                ui->lineEdit_car1_timestamp->setText(QString::number(timeStamp,10));

                //绘制车辆1轨迹

                //第一次全局清除
               if(initial)
               {
                   //   清除上一次的点
                   QString str = "send()";
                   m_pWebView->page()->runJavaScript(str);

                   //把要调用的JS命令当做QString传递给网页
                   QString cmd = QString("add_car_one_marker(%0,%1,%2)").arg(QString::number(bd09_lon,'f', 6)).arg(QString::number(bd09_lat,'f', 6)).arg(id);
                   //qDebug() << cmd;
                    //ui->plainTextEdit->appendPlainText("initial:"+cmd);
                   //实现QT通过C++调用JS函数
                   m_pWebView->page()->runJavaScript(cmd);
                   initial=0;
               }
               else
               {
                   //把要调用的JS命令当做QString传递给网页
                   QString cmd = QString("add_car_one_marker(%0,%1,%2)").arg(QString::number(bd09_lon,'f', 6)).arg(QString::number(bd09_lat,'f', 6)).arg(id);
                   //qDebug() << cmd;
                   // ui->plainTextEdit->appendPlainText("normal:"+cmd);
                   //实现QT通过C++调用JS函数
                   m_pWebView->page()->runJavaScript(cmd);


               }
            }
            //展示车辆2的信息
            else if(id=="22200008")
            {
                //展示信息
                ui->lineEdit_car2_id->setText(id);
                ui->lineEdit_car2_lon->setText(QString::number(lon,10,6));
                ui->lineEdit_car2_lat->setText(QString::number(lat,10,6));
                ui->lineEdit_car2_vel->setText(QString::number(speed,10,6));
                ui->lineEdit_car2_hea->setText(QString::number(direction,10,6));
                ui->lineEdit_car2_timestamp->setText(QString::number(timeStamp,10));

                //绘制车辆2轨迹
                //第一次全局清除
                if(initial)
                {
                    //   清除上一次的点
                    QString str = "send()";
                    m_pWebView->page()->runJavaScript(str);

                    //把要调用的JS命令当做QString传递给网页
                    QString cmd = QString("add_car_two_marker(%0,%1,%2)").arg(QString::number(bd09_lon,'f', 6)).arg(QString::number(bd09_lat,'f', 6)).arg(id);
                    //qDebug() << cmd;
                    //ui->plainTextEdit->appendPlainText(cmd);

                    //实现QT通过C++调用JS函数
                    m_pWebView->page()->runJavaScript(cmd);
                    initial=0;
                }
                else
                {
                    //把要调用的JS命令当做QString传递给网页
                    QString cmd = QString("add_car_two_marker(%0,%1,%2)").arg(QString::number(bd09_lon,'f', 6)).arg(QString::number(bd09_lat,'f', 6)).arg(id);
                    //qDebug() << cmd;
                    //ui->plainTextEdit->appendPlainText(cmd);

                    //实现QT通过C++调用JS函数
                    m_pWebView->page()->runJavaScript(cmd);

                }
            }
        }


    }

}

void MainWindow::on_btnTrace_clicked()
{
    QString cmd = QString("add_car_one_trace();add_car_two_trace();");
     qDebug() << cmd;
     m_pWebView->page()->runJavaScript(cmd);//传给javascript
}

//void MainWindow::on_btnClear_clicked()
//{
//    QString cmd = QString("remove_trace()");
//     qDebug() << cmd;
//     m_pWebView->page()->runJavaScript(cmd);//传给javascript
//}

//获取当前时间戳
long long MainWindow::getTimeStamp(){
       QDateTime time = QDateTime::currentDateTime();   //获取当前时间
       long long timeStamp = time.toMSecsSinceEpoch();//毫秒
       return timeStamp;
}



//以下为84坐标转bd09坐标的函数(省略了通过百度地图在线api转换的过程，直接在qt中转换好发送给百度地图api显示位置)
bool MainWindow::outof_China(double lon, double lat)
{
    return(lon<72.004 || lon>137.8374 || lat<0.8293 || lat >55.8271 || false);
}

POSITION MainWindow::gcj02tobd09(double gcj_lon, double gcj_lat)
{
     double z = sqrt(gcj_lon*gcj_lon + gcj_lat*gcj_lat) + 0.00002*sin(gcj_lat * x_PI);
     double theta = atan2(gcj_lat,gcj_lon) + 0.000003 * cos(gcj_lon * x_PI);
     bd_pos.longitude = z*cos(theta) + 0.0065;
     bd_pos.latitude = z*sin(theta) + 0.006;
     return bd_pos;
}
double MainWindow::translate_lon(double lon, double lat)
{
    double ret = 300.0 + lon +2.0*lat + 0.1*lon*lon +0.1*lon*lat + 0.1*sqrt(abs(lon));
    ret += (20.0 * sin(6.0*lon*PI) + 20.0*sin(2.0*lon*PI)) *2.0 / 3.0;
    ret += (20.0 * sin(lon*PI) + 40.0*sin(lon/3.0 *PI)) *2.0 /3.0;
    ret += (150 * sin(lon/12.0 *PI) + 300.0*sin(lon/30.0 * PI)) *2.0 /3.0;
    return ret;
}
double MainWindow::translate_lat(double lon, double lat)
{
    double ret = -100 + 2.0*lon + 3.0*lat + 0.2*lat*lat + 0.1*lon*lat + 0.2*sqrt((abs(lon)));
    ret += (20.0 *sin(6.0*lon*PI) + 20*sin(2.0*lon*PI)) *2.0 /3.0;
    ret += (20.0 *sin(lat*PI) + 40.0*sin(lat/3.0*PI)) *2.0 /3.0;
    ret += (160.0*sin(lat/12.0*PI) + 320.0*sin(lat/30.0 *PI)) *2.0 /3.0;
    return ret;
}

POSITION MainWindow::wgs84togcj02(double wgs_lon, double wgs_lat)
{
    if(outof_China(wgs_lon,wgs_lat))
    {
        gcj_pos.longitude = wgs_lon;
        gcj_pos.latitude = wgs_lat;
        return gcj_pos;
    }
    else
    {
        double dlat = translate_lat(wgs_lon - 105.0,wgs_lat - 35.0);
        double dlon = translate_lon(wgs_lon - 105.0,wgs_lat - 35.0);
        double radlat = wgs_lat/180.0 * PI;
        double magic = sin(radlat);
        magic = 1 - ee*magic*magic;
        double squrtmagic = sqrt(magic);
        dlon = (dlon *180.0)/(a/squrtmagic*cos(radlat)*PI);
        dlat = (dlat *180.0)/((a*(1-ee))/(magic * squrtmagic)*PI);
        gcj_pos.longitude = wgs_lon + dlon;
        gcj_pos.latitude = wgs_lat +dlat;
        return gcj_pos;
    }
}

//展示当前两车距离
void MainWindow::showDistance(double distance)
{
    ui->lineEdit_distance->setText(QString::number(distance,10,6));
    if(distance<5)
    {
        QPalette palette;
        palette.setColor(QPalette::Text, QColor(255,0,0));//距离阈值小于5m用红色提醒
        ui->lineEdit_showWarning->setPalette(palette);
        ui->lineEdit_showWarning->setText("YES");
    }
    else
    {
        QPalette palette;
        palette.setColor(QPalette::Text, QColor(0,0,0));//NO用黑色提醒
        ui->lineEdit_showWarning->setPalette(palette);
        ui->lineEdit_showWarning->setText("NO");
    }

}

void MainWindow::showWarning(QJsonObject message)
{
//    QPalette palette;
//    palette.setColor(QPalette::Text, QColor(255,0,0));
//    ui->lineEdit_showWarning->setPalette(palette);
//    ui->lineEdit_showWarning->setText("YES");
}

//通过Udp向50800端口发送预警信息
void MainWindow::sendCollisionWarning(QJsonObject message)
{
    udpTargetAddr.setAddress("192.168.20.199");
    udpTargetPort = 50800;
    myudp->sendMessage(requestAddr,udpTargetPort,message);
}

void MainWindow::appendText(QString text)
{
    ui->plainTextEdit->appendPlainText(text);
}

void MainWindow::on_pushButton_2_clicked()
{
//    //udp设置
//    //设置端口号为50501
//    udpListenPort=50501;
//    //设置ip地址
//    localAddr.setAddress("192.168.20.224");
//    //qDebug()<<localAddr<<"+"<<udpListenPort;
//    //绑定udp连接
//    myudp->bindPort(localAddr,udpListenPort);
//    //创建json对象
//    udpTargetAddr.setAddress("192.168.20.199");
//    udpTargetPort = 50800;

//    long long timestamp=getTimeStamp();
//    QString timestamp_string=QString::number(timestamp);
//    qDebug()<<timestamp_string;
//    qDebug()<<timestamp;




//    // 创建事件列表中的事件对象
//      QJsonObject evtObj;
//      evtObj["ObjId"] = "O20201013172150160020200521AIE44063472379";
//      evtObj["ID"] = "E2020093018001402300R33101001917000190123";
//      evtObj["EvtStatus"] = 0;
//      evtObj["EvtType"] = 2;
//      evtObj["Lon"] = 107.7868827;
//      evtObj["Lat"] = 29.9157172;
//      evtObj["Ele"] = 35.4;
//      evtObj["XPos"] = 3.5;
//      evtObj["YPos"] = 60.2;
//      evtObj["VehL"] = 4.6;
//      evtObj["VehW"] = 2.3;
//      evtObj["VehH"] = 1.6;

//      // 创建事件列表数组
//      QJsonArray evtList;
//      evtList.append(evtObj);

//      // 创建数据对象
//      QJsonObject dataObj;
//      dataObj["MsgType"] = 2012;
//      dataObj["DevNo"] = "20200521AIE44063472";
//      dataObj["MecNo"] = "128050080002";
//      dataObj["Timestamp"] = "2022-06-09 17:21:52.334";
//      dataObj["Evt_List"] = evtList;

//      // 创建JSON对象
//      QJsonObject jsonObj;
//      jsonObj["MsgType"] = 2012;
//      jsonObj["DevNo"] = "20200521AIE44063472";
//      jsonObj["MecNo"] = "128050080002";
//      jsonObj["Timestamp"] = "2022-06-09 17:21:52.334";
//      jsonObj["Evt_List"] = evtList;

//    myudp->sendMessage(requestAddr,udpTargetPort,jsonObj);
}


