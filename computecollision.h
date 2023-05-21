#ifndef COMPUTECOLLISION_H
#define COMPUTECOLLISION_H

#include <QThread>
#include <QJsonDocument>
#include <QJsonObject>
#include <QStringList>
#include <QJsonArray>
class ComputeCollision : public QThread
{
    Q_OBJECT
public:
    explicit ComputeCollision(QObject *parent = nullptr);

    double getDistance(QJsonObject nodeOne, QJsonObject nodeTwo);

    QJsonObject createWarningJson();

    //预测轨迹点
    QJsonObject predictTrajectory(double unittime, double v, double a, double dir, double vlat, double vlon);

public slots:
    void recvJsonMessage(QJsonObject message,long long timeStamp);//用于接收主线程传过来的bsm信息和时间戳 (槽函数)
    void computeData(QJsonObject message,long long timestamp);
protected:
    void run() override;//重写基函数run()

signals:
    void sendDistance(double distance);//发送当前距离信号
    void sendWarning(QJsonObject warning);//发送预警信号
    void startCompute(QJsonObject message,long long timestamp);
    void sendDebug(QString text);

private:
    QJsonObject m_message;
    bool initial=1;//初始化参数
    double pre_car_one_spd;//上一次车辆1的速度
    double pre_car_two_spd;//上一次车辆2的速度
    long long pre_time;//上一次的时间
    double delta_time_s;//将时间戳差值单位转换为秒


    int idOne;
    int idTwo;
    bool warningTag=false;
    bool warningStatusTwo;

//    const int FREQUENCY = 1000;   // timer的时间频率
//    const int THRESHOLD = 1000;  // 碰撞时间的阈值
//    const int UNITTIME = 10;     // 求轨迹的单位时间


    const int FREQUENCY = 200;    //timer的时间频率（单位：毫秒）
    double TIME_THRESHOLD = 5;   //碰撞时间的阈值（单位：秒）
    double DISTANCE_THRESHOLD = 5; //碰撞区域的直径（单位：米）
    const int UNITTIME = 1;     //轨迹的单位间隔时间（单位：毫秒）
    const double DIR = 10;        //方向的阈值（单位：度）
    const double Velocity = 10;   //速度的阈值（单位：m/ms）
    const double Distance = 10;   //距离的阈值（单位：m）
    const double VTIME = 200;     //行驶时间的阈值（单位：毫秒）
    const double Acceleration = 10; //加速度的阈值（单位：毫秒）
    const double LAT= 0.0000001; //纬度阈值（单位：度）
    const double LON= 0.0000001; //经度阈值（单位：度）
    const double lat_THRESHOLD = 0.001;//纬度的精度
    const double lon_THRESHOLD = 0.01;//经度的精度

    double EARTH_RADIUS = 6371.0;
    double PI = 3.1415926;
    double distance = 0.0;


};

#endif // COMPUTECOLLISION_H
