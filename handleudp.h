#ifndef HANDLEUDP_H
#define HANDLEUDP_H

#include <QUdpSocket>
#include <QJsonObject>
#include <QJsonDocument>

class handleUdp: public QUdpSocket
{
public:
    explicit handleUdp(QObject *parent = nullptr);
    //绑定端口
    bool bindPort(QHostAddress addr, qint16 port);
    //去除绑定
    void unbindPort();

  //信号
  signals:
    //接受新消息，参数为IP地址，与消息内容
    void resceiveMessage(const QString &from, const QJsonObject &message);

  //槽函数
  public slots:
    //读取信息的槽函数
    void readyRead();
    //发送信息的槽函数
    //sender是Host地址，senderPort是接收端的端口号，string是发送的内容
    void sendMessage(QHostAddress sender, quint16 senderPort, QJsonObject result);

  private:
    //使用QUdpSocket 进行发送接收
    QUdpSocket *socket;
};

#endif // HANDLEUDP_H
