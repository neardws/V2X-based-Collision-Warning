#include "handleudp.h"


//构造函数
handleUdp::handleUdp(QObject *parent): QUdpSocket(parent)
{
     socket = new QUdpSocket();
}

//绑定端口 需要本地IP地址和接收端端口号两个参数
bool handleUdp::bindPort(QHostAddress addr, qint16 port)
{
    socket->abort();
    bool isBinded = socket->bind(addr, port);
    if (isBinded)
        connect(socket, SIGNAL(readyRead()), this, SLOT(readMessage()));

    return isBinded;
}

//解绑端口
void handleUdp::unbindPort()
{
    disconnect(socket, SIGNAL(readyRead()), this, SLOT(readyRead()));  //关闭连接
    socket->close();  //关闭socket
}

//读取信息
void handleUdp::readMessage()
{
    //QUdpSocket发送的数据报是QByteArray类型的字节数组
    QByteArray datagram;
    //pendingDatagramsize()   返回数据的数据报字节数
    datagram.resize (socket->pendingDatagramSize());

    QHostAddress peerAddr;
    quint16 peerPort;

    //读取数据报信息：
    //参数data和 maxSize是必须的。address和 port变量是可选的，用于获取数据报来源的地址和端口
    socket->readDatagram (datagram.data(), datagram.size(), &peerAddr, &peerPort);

    //将datagram转换为json对象
    QJsonObject message = QJsonDocument::fromJson(datagram).object();

    //发送消息信号
   // emit newMessage(peerAddr.toString(), message);

}

//发送信息
void handleUdp::sendMessage(QHostAddress sender, quint16 senderPort, QJsonObject result)
{
    //QUdpSocket发送的数据报是QByteArray类型的字节数组
    QByteArray datagram = QJsonDocument(result).toJson();

    //发出数据报
    //参数为数据报、目标地址、目标端口
    socket->writeDatagram(datagram, sender, senderPort);
}
