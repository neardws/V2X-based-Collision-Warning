#include "handleudp.h"


//构造函数
handleUdp::handleUdp(QObject *parent): QUdpSocket(parent)
{
     socket = new QUdpSocket();
}

//绑定端口
bool handleUdp::bindPort(QHostAddress addr, qint16 port)
{
    socket->abort();
    bool isBinded = socket->bind(addr, port);
    if (isBinded)
        connect(socket, SIGNAL(readyRead()), this, SLOT(readyRead()));

    return isBinded;
}

//解绑端口
void handleUdp::unbindPort()
{
    disconnect(socket, SIGNAL(readyRead()), this, SLOT(readyRead()));  //关闭连接
    socket->close();  //关闭socket
}

void handleUdp::readyRead()
{

}

void handleUdp::sendMessage(QHostAddress sender, quint16 senderPort, QJsonObject result)
{
    QByteArray message = QJsonDocument(result).toJson();
    socket->writeDatagram(message, sender, senderPort);
}
