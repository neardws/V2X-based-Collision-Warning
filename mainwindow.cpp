#include "mainwindow.h"
#include "ui_mainwindow.h"

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    ui->axWidget->setControl(QString::fromUtf8("{8856F961-340A-11D0-A96B-00C04FD705A2}"));//注册组件ID
    ui->axWidget->setProperty("DisplayAlerts",false);//不显示警告信息
    ui->axWidget->setProperty("DisplayScrollBars",true);//不显示滚动条
    //QString webstr=QString("file:///E:/map/mymap.html");//设置要打开的网页
    QString webstr=QString("https://map.baidu.com/@11861286,3423481,13z");//设置要打开的网页
    ui->axWidget->dynamicCall("Navigate(const QString&)",webstr);//显示网页
}

//获取时间戳
qint64 MainWindow::getTime()
{
    //获取当前时间
    QDateTime dateTime = QDateTime::currentDateTime();
    //转换为毫秒
    qint64 epochTime = dateTime.toMSecsSinceEpoch();
    return epochTime;
}

MainWindow::~MainWindow()
{
    delete ui;
}


//绑定端口
bool MainWindow::createConnection()
{

    bool isConnected = false;
    //获取ip地址
    localAddr.setAddress(ui->lineEdit_locIp->text());

    //获取端口号
    if (ui->lineEditSetPort->text() != ""){
        localPort = ui->lineEditSetPort->text().toInt();
    }

    //绑定端口,并返回是否绑定成功
    isConnected = handleudp->bindPort(localAddr, localPort);
    return isConnected;
}


void MainWindow::on_btn_start_clicked()
{
    if(isUdpStarted){
        emit newLogInfo("UDP线程已启动。");
    } else{
        disconnect(ui->btn_start, SIGNAL(clicked()), this, SLOT(on_btn_start_clicked()()));

        //开始监听，调用createConnection绑定端口创建连接
        if(createConnection()){
            //ui->textLog->appendPlainText("UDP正在监听 "+ localAddr.toString()+ ":"+ QString::number(localPort));
            //重新绑定槽函数
            connect(ui->btn_start, SIGNAL(clicked()),this,SLOT(onUdpStopButtonClicked()));
            //当点击开始监听之后，按钮名改为"停止监听"
            ui->btn_start->setText("停止监听");

            //绑定接收到消息的槽函数
            connect(handleudp, SIGNAL(newMessage(QString, QJsonObject)), this, SLOT(onUdpAppendMessage(QString, QJsonObject)));
        }
        else{
            //ui->textLog->appendPlainText("UDP监听失败:"+ localAddr.toString()+ ":"+ QString::number(localPort));
            connect(ui->btn_start, SIGNAL(clicked()), this, SLOT(on_btn_start_clicked()()));
        }
        isUdpStarted = true;
    }
}

void MainWindow::onUdpStopButtonClicked(){
    if (isUdpStarted){
        //如果udp在启动中就解除连接
        disconnect(ui->btn_start, SIGNAL(clicked()), this, SLOT(onUdpStopButtonClicked()));
        //ui->textLog->appendPlainText("UDP 停止监听.");
        //解除槽函数绑定
        disconnect(handleudp, SIGNAL(newMessage(QString, QJsonObject)), this, SLOT(onUdpAppendMessage(QString, QJsonObject)));
        ui->btn_start->setText("开始监听");
        handleudp->unbindPort();
        //重新绑定槽函数
        connect(ui->btn_start, SIGNAL(clicked()), this, SLOT(on_btn_start_clicked()));
        isUdpStarted = false;
    } else{
        emit newLogInfo("UDP线程已停止。");
    }

}

void MainWindow::onUdpAppendMessage(const QString &from, const QJsonObject &message)
{

}
