#include "computecollision.h"
#include <QDebug>
ComputeCollision::ComputeCollision(QObject *parent) : QThread(parent)
{

}

void ComputeCollision::recvJsonMessage(QJsonObject message,long long timeStamp)
{
    //message=message;//存放bsm信息
    //如果是第一次进来的消息，pre_time就赋值为时间戳，如果不是第一次，就减去上次的时间保留差值
//    if(initial)
//    {
//        pre_time=timeStamp;//存放时间戳
//    }
//    else
//    {
//        pre_time=timeStamp-pre_time;//时间戳相减单位为毫秒
//        delta_time_s=pre_time*0.001;//单位转换，将毫秒转换为秒
//    }

    //qDebug()<<"已接收到信息";
    //emit sendDebug("have received");
    emit startCompute(message,timeStamp);
   // computeData(message);

}

//根据经纬度计算两点间距离
double ComputeCollision::getDistance(QJsonObject nodeOne, QJsonObject nodeTwo)
{
        //获取第一辆车的经纬度
        double lata = nodeOne.find("lat").value().toDouble();
        double loga = nodeOne.find("lon").value().toDouble();

        //获取第二辆车的经纬度
        double latb = nodeTwo.find("lat").value().toDouble();
        double logb = nodeTwo.find("lon").value().toDouble();

       double lat_a = 0.0;
       double lat_b = 0.0;
       double log_a = 0.0;
       double log_b = 0.0;

       //转弧度
       lat_a = lata  * PI / 180;
       lat_b = latb  * PI / 180;
       log_a = loga  * PI / 180;
       log_b = logb  * PI / 180;

       double dis = cos(lat_b) * cos(lat_a) * cos(log_b -log_a) + sin(lat_a) * sin(lat_b);

       distance = EARTH_RADIUS * acos(dis);
       return distance*1000;
}

//封装预警信息(按照前方拥堵信息的格式进行封装)
QJsonObject ComputeCollision::createWarningJson()
{
    // 创建事件列表中的事件对象
      QJsonObject evtObj;
      evtObj["ObjId"] = "O20201013172150160020200521AIE44063472379";
      evtObj["ID"] = "E2020093018001402300R33101001917000190123";
      evtObj["EvtStatus"] = 0;
      evtObj["EvtType"] = 2;
      evtObj["Lon"] = 107.7868827;
      evtObj["Lat"] = 29.9157172;
      evtObj["Ele"] = 35.4;
      evtObj["XPos"] = 3.5;
      evtObj["YPos"] = 60.2;
      evtObj["VehL"] = 4.6;
      evtObj["VehW"] = 2.3;
      evtObj["VehH"] = 1.6;

      // 创建事件列表数组
      QJsonArray evtList;
      evtList.append(evtObj);

      // 创建数据对象
      QJsonObject dataObj;
      dataObj["MsgType"] = 2012;
      dataObj["DevNo"] = "20200521AIE44063472";
      dataObj["MecNo"] = "128050080002";
      dataObj["Timestamp"] = "2022-06-09 17:21:52.334";
      dataObj["Evt_List"] = evtList;

      // 创建JSON对象
      QJsonObject jsonObj;
      jsonObj["MsgType"] = 2012;
      jsonObj["DevNo"] = "20200521AIE44063472";
      jsonObj["MecNo"] = "128050080002";
      jsonObj["Timestamp"] = "2022-06-09 17:21:52.334";
      jsonObj["Evt_List"] = evtList;

      return jsonObj;
}

//预测轨迹
QJsonObject ComputeCollision::predictTrajectory(double unittime, double v, double a, double dir, double vlat, double vlon)
{
    //double l,h,d,lon,lat;    //小三角形斜边长,高，底边长，当前所在经纬度
    QJsonObject array;       //每个时间节点的经纬度

    int t0 = unittime;       //时间间隔
    //x=v0*t+1/2*a*t^2
//    l=(v*t0+1/2*a*pow(t0,2))/100000;    //计算斜边长x=v0t+1/2*a*t^2
//    h=l*cos(dir);            //计算纬度差
//    d=l*sin(dir);            //计算经度差
//    lat=vlat+h;             //计算间隔t0后的纬度
//    lon=vlon+d;             //计算间隔t0后的经度

    double l=(v*t0+1/2*a*pow(t0,2));
    double arc = 6371.393 * 1000;
    vlat += l * cos(dir) / (arc * 2 * PI / 360);
    vlon += l * sin(dir) / (arc * cos(vlat * PI /	180) * 2 * PI / 360);

    //将当前经纬度保存进QJsonObject
    array.insert("lat",vlat);
    array.insert("lon",vlon);

    //emit newLogInfo(QJsonDocument(array).toJson()+ "\n");
    return array;
}

void ComputeCollision::computeData(QJsonObject message,long long timestamp)
{
    QJsonObject nodeOne, nodeTwo;

    //解析JSON
    QJsonValue data=message.find("data").value();
    QJsonArray dataArr=message.find("data").value().toArray();
        //提取第1辆车的信息(id,经度、纬度、方向、速度)
    QJsonValue datachild1 = dataArr.at(0);
    QJsonObject dataobj1=datachild1.toObject();
    QString      id1               = dataobj1.find("device_id").value().toString();
    double       dir1              = dataobj1.find("hea").value().toDouble()* PI /180; //角度转弧度
    double       lat1              = dataobj1.find("lat").value().toDouble();
    double       lon1              = dataobj1.find("lon").value().toDouble();
    double       speed1            = dataobj1.find("spd").value().toDouble()/3.6; // km/h->m/s

        //提取第2辆车的信息(id,经度、纬度、方向、速度)
    QJsonValue datachild2 = dataArr.at(1);
    QJsonObject dataobj2=datachild2.toObject();
    QString      id2               = dataobj2.find("device_id").value().toString();
    double       dir2              = dataobj2.find("hea").value().toDouble()* PI /180;//角度转弧度
    double       lat2              = dataobj2.find("lat").value().toDouble();
    double       lon2              = dataobj2.find("lon").value().toDouble();
    double       speed2            = dataobj2.find("spd").value().toDouble()/3.6;// km/h->m/s


    //封装两个json用于计算距离
    nodeOne.insert("lat", lat1);
    nodeOne.insert("lon", lon1);
    nodeTwo.insert("lat", lat2);
    nodeTwo.insert("lon", lon2);

    //第一次进来的数据，由于无法计算加速度，则只计算当前位置是否会发生碰撞，无需预测几秒后的轨迹点
    if(initial)
    {
        pre_time=timestamp;//存放时间戳
        //存放两辆车的速度为下一次计算加速度做准备
        pre_car_one_spd=speed1;
        pre_car_two_spd=speed2;
        initial=0;
        double initial_distance=getDistance(nodeOne,nodeTwo);
        emit sendDistance(initial_distance);//发送给主线程展示当前距离
        //小于阈值则发送预警
        if(initial_distance<=DISTANCE_THRESHOLD)
        {
            //发送预警消息
            QJsonObject warningJson=createWarningJson();
            emit sendWarning(warningJson);
        }

    }
    else{
        double delta_time_s=(timestamp-pre_time)*0.001;//时间戳相减单位为毫秒,单位转换，将毫秒转换为秒
        pre_time=timestamp;
        //计算加速度
        double acc1=(speed1-pre_car_one_spd)/(delta_time_s);//计算车辆1的加速度
        double acc2=(speed2-pre_car_two_spd)/(delta_time_s);//计算车辆2的加速度
        //存放两辆车的速度为下一次计算加速度做准备
        pre_car_one_spd=speed1;
        pre_car_two_spd=speed2;
        //对初始点单独进行判断，计算初始点的位置是否会发生碰撞
        double initial_distance=getDistance(nodeOne,nodeTwo);

        emit sendDistance(initial_distance);//发送给主线程展示当前距离

        //小于阈值则发送预警
        if(initial_distance<=DISTANCE_THRESHOLD)
        {
            //发送预警消息
            QJsonObject warningJson=createWarningJson();
            emit sendWarning(warningJson);
        }
        else
        {
            double distance;
            //THRESHOLD时间阈值为5s,计算之后的五秒内是否会发生碰撞
            for(int i = 1; i <= TIME_THRESHOLD; i++){
                //计算两辆车在第i秒后的位置
                nodeOne = predictTrajectory(i,speed1,acc1,dir1,lat1,lon1);//预测下一秒的轨迹应该出现在什么点
                nodeTwo = predictTrajectory(i,speed2,acc2,dir2,lat2,lon2);
                //通过两辆车下一秒所在的位置计算预测距离
                distance = getDistance(nodeOne, nodeTwo);
                //emit sendDistance(initial_distance);
                if (distance <= DISTANCE_THRESHOLD){
                    //发送预警消息
                    QJsonObject warningJson=createWarningJson();
                    emit sendWarning(warningJson);
                    return;
                }
            }
        }
    }
}

void ComputeCollision::run()
{
//    qDebug()<<"计算线程已启动";
    //接收函数发送startCompute信号，调用computeData槽函数
    connect(this,SIGNAL(startCompute(QJsonObject,long long)),this,SLOT(computeData(QJsonObject,long long)));
    this->exec();
}
