#include "mainwindow.h"
#include "ui_mainwindow.h"

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);
}

//获取时间戳
qint64 MainWindow::getTime()
{
    QDateTime dateTime = QDateTime::currentDateTime();
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
    localAddr.setAddress(ui->lineEdit_locIp->text());

    if (ui->lineEditSetPort->text() != ""){
        localPort = ui->lineEditSetPort->text().toInt();
    }
    isConnected = handleudp->bindPort(localAddr, localPort);
    return isConnected;
}


void MainWindow::on_btn_start_clicked()
{
    if(isUdpStarted){
        emit newLogInfo("UDP线程已启动。");
    } else{
        disconnect(ui->btn_start, SIGNAL(clicked()), this, SLOT(on_btn_start_clicked()()));

        //开始监听
        if(createConnection()){
            ui->textLog->appendPlainText("UDP正在监听 "+ localAddr.toString()+ ":"+ QString::number(localPort));
            //重新绑定槽函数
            connect(ui->btn_start, SIGNAL(clicked()),this,SLOT(onUdpStopButtonClicked()));
            ui->btn_start->setText("停止监听");

            //绑定接收到消息的槽函数
            connect(handleudp, SIGNAL(newMessage(QString, QJsonObject)), this, SLOT(onUdpAppendMessage(QString, QJsonObject)));
        }
        else{
            ui->textLog->appendPlainText("UDP监听失败:"+ localAddr.toString()+ ":"+ QString::number(localPort));
            connect(ui->btn_start, SIGNAL(clicked()), this, SLOT(on_btn_start_clicked()()));
        }
        isUdpStarted = true;
    }
}

void MainWindow::onUdpStopButtonClicked(){
    if (isUdpStarted){
        disconnect(ui->btn_start, SIGNAL(clicked()), this, SLOT(onUdpStopButtonClicked()));
        ui->textLog->appendPlainText("UDP Stoped.");
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
