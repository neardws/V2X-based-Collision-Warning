#ifndef MYBRIDGEOFJS_H
#define MYBRIDGEOFJS_H

#include <QObject>
#include <QJSValue>
#include <functional>

class MybridgeofJS : public QObject
{
    Q_OBJECT
public:
    explicit MybridgeofJS(QObject *parent = nullptr);

signals:
    //void sigJSMsg(const QString &name);

public slots:
    //void getJSMsg();
    void getCoordinate(QString lon,QString lat);

};

#endif // MYBRIDGEOFJS_H
