package car.bkrc.com.car2021.DataProcessingModule;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import car.bkrc.com.car2021.Utils.CameraUtile.XcApplication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import car.bkrc.com.car2021.MessageBean.DataRefreshBean;
import car.bkrc.com.car2021.Utils.OtherUtil.SerialPort;

import car.bkrc.com.car2021.ActivityView.FirstActivity;

/**
 * Socket数据处理类
 */
public class ConnectTransport {
    public static DataInputStream bInputStream = null;
    public static DataOutputStream bOutputStream = null;
    public static Socket socket = null;
    public byte[] rbyte = new byte[50];
    private Handler reHandler;
    public short TYPE = 0xAA;
    public short TYPE2 = 0xBB;
    public short MAJOR = 0x00;
    public short FIRST = 0x00;
    public short SECOND = 0x00;
    public short THRID = 0x00;
    public short CHECKSUM = 0x00;

    private static OutputStream SerialOutputStream;
    private InputStream SerialInputStream;
    private boolean Firstdestroy = false;  ////Firstactivity 是否已销毁了

    public void destory() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                bInputStream.close();
                bOutputStream.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void connect(Handler reHandler, String IP) {
        try {
            this.reHandler = reHandler;
            Firstdestroy = false;
            int port = 60000;
            socket = new Socket(IP, port);
            bInputStream = new DataInputStream(socket.getInputStream());
            bOutputStream = new DataOutputStream(socket.getOutputStream());
            if (!inputDataState) {
                reThread();
            }
            EventBus.getDefault().post(new DataRefreshBean(3));
        } catch (SocketException ignored) {
            EventBus.getDefault().post(new DataRefreshBean(4));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void serial_connect(Handler reHandler) {
        this.reHandler = reHandler;
        try {
            int baudrate = 115200;
            String path = "/dev/ttyS4";
            SerialPort mSerialPort = new SerialPort(new File(path), baudrate, 0);
            SerialOutputStream = mSerialPort.getOutputStream();
            SerialInputStream = mSerialPort.getInputStream();
            //new Thread(new SerialRunnable()).start();
            //reThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        XcApplication.executorServicetor.execute(new SerialRunnable());
        //new Thread(new serialRunnable()).start();
    }

    byte[] serialreadbyte = new byte[50];

    class SerialRunnable implements Runnable {
        @Override
        public void run() {
            while (SerialInputStream != null) {
                try {
                    int num = SerialInputStream.read(serialreadbyte);
                    // String  readserialstr =new String(serialreadbyte);
                    String readserialstr = new String(serialreadbyte, 0, num, "utf-8");
                    Log.e("----serialreadbyte----", "******" + readserialstr);
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = serialreadbyte;
                    reHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean inputDataState = false;

    private void reThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto1-generated method stub
                while (socket != null && !socket.isClosed()) {
                    if (Firstdestroy == true)  //Firstactivity 已销毁了
                    {
                        break;
                    }
                    try {
                        inputDataState = true;
                        bInputStream.read(rbyte);
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = rbyte;
                        reHandler.sendMessage(msg);
                    } catch (SocketException ignored) {
                        EventBus.getDefault().post(new DataRefreshBean(4));
                        destory();
                        inputDataState = false;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        EventBus.getDefault().post(new DataRefreshBean(4));
                        destory();
                        inputDataState = false;
                    } catch (UnsupportedOperationException ignored) {
                        inputDataState = false;
                    }
                }
            }
        }).start();

    }

    private void send() {
        CHECKSUM = (short) ((MAJOR + FIRST + SECOND + THRID) % 256);
        // 发送数据字节数组

        final byte[] sbyte = {0x55, (byte) TYPE, (byte) MAJOR, (byte) FIRST, (byte) SECOND, (byte) THRID, (byte) CHECKSUM, (byte) 0xBB};

        if (XcApplication.isserial == XcApplication.Mode.SOCKET) {
            XcApplication.executorServicetor.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        if (socket != null && !socket.isClosed()) {
                            bOutputStream.write(sbyte, 0, sbyte.length);
                            bOutputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (XcApplication.isserial == XcApplication.Mode.SERIAL) {

            XcApplication.executorServicetor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        SerialOutputStream.write(sbyte, 0, sbyte.length);
                        SerialOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (XcApplication.isserial == XcApplication.Mode.USB_SERIAL)
            try {
                FirstActivity.sPort.write(sbyte, 5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ignored) {

            }
    }

    private void sendSecend() {
        CHECKSUM = (short) ((MAJOR + FIRST + SECOND + THRID) % 256);
        // 发送数据字节数组

        final byte[] sbyte = {0x55, (byte) TYPE2, (byte) MAJOR, (byte) FIRST, (byte) SECOND, (byte) THRID, (byte) CHECKSUM, (byte) 0xBB};

        if (XcApplication.isserial == XcApplication.Mode.SOCKET) {
            XcApplication.executorServicetor.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        if (socket != null && !socket.isClosed()) {
                            bOutputStream.write(sbyte, 0, sbyte.length);
                            bOutputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (XcApplication.isserial == XcApplication.Mode.SERIAL) {

            XcApplication.executorServicetor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        SerialOutputStream.write(sbyte, 0, sbyte.length);
                        SerialOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (XcApplication.isserial == XcApplication.Mode.USB_SERIAL)
            try {
                FirstActivity.sPort.write(sbyte, 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void send_voice(final byte[] textbyte) {
        if (XcApplication.isserial == XcApplication.Mode.SOCKET) {
            XcApplication.executorServicetor.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        if (socket != null && !socket.isClosed()) {
                            bOutputStream.write(textbyte, 0, textbyte.length);
                            bOutputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (XcApplication.isserial == XcApplication.Mode.SERIAL) {

            XcApplication.executorServicetor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        SerialOutputStream.write(textbyte, 0, textbyte.length);
                        SerialOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (XcApplication.isserial == XcApplication.Mode.USB_SERIAL)
            try {
                FirstActivity.sPort.write(textbyte, 5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ignored) {
                Log.e("UART:", "unline");
            }
    }


    // 前进
    public void go(int sp_n, int en_n) {
        MAJOR = 0x02;
        FIRST = (byte) (sp_n & 0xFF);
        SECOND = (byte) (en_n & 0xff);
        THRID = (byte) (en_n >> 8);
        send();
    }

    // 后退
    public void back(int sp_n, int en_n) {
        MAJOR = 0x03;
        FIRST = (byte) (sp_n & 0xFF);
        SECOND = (byte) (en_n & 0xff);
        THRID = (byte) (en_n >> 8);
        send();
    }


    //左转
    public void left(int sp_n) {
        MAJOR = 0x04;
        FIRST = (byte) (sp_n & 0xFF);
        SECOND = (byte) 0x00;
        THRID = (byte) 0x00;
        send();
    }


    // 右转
    public void right(int sp_n) {
        MAJOR = 0x05;
        FIRST = (byte) (sp_n & 0xFF);
        SECOND = (byte) 0x00;
        THRID = (byte) 0x00;
        send();
    }

    // 停车
    public void stop() {
        MAJOR = 0x01;
        FIRST = 0x00;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    // 程序自动执行
    public void autoDrive() {
        MAJOR = 0xA0;
        FIRST = 0x00;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    // 程序自动执行
    public void erweima(byte[] data) {
        MAJOR = 0xA0;
        FIRST = data[0];
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    // 循迹
    public void line(int sp_n) {  //寻迹
        MAJOR = 0x06;
        FIRST = (byte) (sp_n & 0xFF);
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    //清除码盘值
    public void clear() {
        MAJOR = 0x07;
        FIRST = 0x00;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    public void stateChange(final int i) {//主从车状态转换
        final short temp = TYPE;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (i == 1) {//从车状态
                    TYPE = 0x02;
                    MAJOR = 0x80;
                    FIRST = 0x01;
                    SECOND = 0x00;
                    THRID = 0x00;
                    send();
                    yanchi(500);

                    TYPE = (byte) 0xAA;
                    MAJOR = 0x80;
                    FIRST = 0x01;
                    SECOND = 0x00;
                    THRID = 0x00;
                    send();
                    TYPE = 0x02;
                } else if (i == 2) {// 主车状态
                    TYPE = 0x02;
                    MAJOR = 0x80;
                    FIRST = 0x00;
                    SECOND = 0x00;
                    THRID = 0x00;
                    send();
                    yanchi(500);

                    TYPE = (byte) 0xAA;
                    MAJOR = 0x80;
                    FIRST = 0x00;
                    SECOND = 0x00;
                    THRID = 0x00;
                    send();
                    TYPE = 0xAA;
                }
                TYPE = temp;
            }
        }).start();
    }

    // 红外
    public void infrared(final byte one, final byte two, final byte thrid, final byte four, final byte five,
                         final byte six) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MAJOR = 0x10;
                FIRST = one;
                SECOND = two;
                THRID = thrid;
                send();
                yanchi(500);
                MAJOR = 0x11;
                FIRST = four;
                SECOND = five;
                THRID = six;
                send();
                yanchi(500);
                MAJOR = 0x12;
                FIRST = 0x00;
                SECOND = 0x00;
                THRID = 0x00;
                send();
                yanchi(1000);
            }
        }).start();
    }

    // 双色led灯
    public void lamp(byte command) {
        MAJOR = 0x40;
        FIRST = command;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    // 指示灯
    public void light(int left, int right) {
        if (left == 1 && right == 1) {
            MAJOR = 0x20;
            FIRST = 0x01;
            SECOND = 0x01;
            THRID = 0x00;
            send();
        } else if (left == 1 && right == 0) {
            MAJOR = 0x20;
            FIRST = 0x01;
            SECOND = 0x00;
            THRID = 0x00;
            send();
        } else if (left == 0 && right == 1) {
            MAJOR = 0x20;
            FIRST = 0x00;
            SECOND = 0x01;
            THRID = 0x00;
            send();
        } else if (left == 0 && right == 0) {
            MAJOR = 0x20;
            FIRST = 0x00;
            SECOND = 0x00;
            THRID = 0x00;
            send();
        }
    }


    // 蜂鸣器
    public void buzzer(int i) {
        if (i == 1)
            FIRST = 0x01;
        else if (i == 0)
            FIRST = 0x00;
        MAJOR = 0x30;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    /**
     * 从车二维码识别
     */
    public void qr_rec(int state) {
        FIRST = 0x92;
        MAJOR = (byte) state;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    public void gear(int i) {// 加光照档位
        if (i == 1)
            MAJOR = 0x61;
        else if (i == 2)
            MAJOR = 0x62;
        else if (i == 3)
            MAJOR = 0x63;
        FIRST = 0x00;
        SECOND = 0x00;
        THRID = 0x00;
        send();
    }

    //立体显示
    public void infrared_stereo(final short[] data) {
        MAJOR = 0x10;
        FIRST = 0xff;
        SECOND = data[0];
        THRID = data[1];
        send();
        yanchi(500);
        MAJOR = 0x11;
        FIRST = data[2];
        SECOND = data[3];
        THRID = data[4];
        send();
        yanchi(500);
        MAJOR = 0x12;
        FIRST = 0x00;
        SECOND = 0x00;
        THRID = 0x00;
        send();
        yanchi(700);
    }

    //立体显示
    public void infrared_dis(final short[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MAJOR = 0x10;
                FIRST = 0xff;
                SECOND = data[0];
                THRID = data[1];
                send();
                yanchi(500);
                MAJOR = 0x11;
                FIRST = data[2];
                SECOND = data[3];
                THRID = data[4];
                send();
                yanchi(500);
                MAJOR = 0x12;
                FIRST = 0x00;
                SECOND = 0x00;
                THRID = 0x00;
                send();
                yanchi(500);
            }
        }).start();
    }


    //智能交通灯
    public void traffic_control(int type, int major, int first) {
        byte temp = (byte) TYPE;
        TYPE = (short) type;
        MAJOR = (byte) major;
        FIRST = (byte) first;
        SECOND = 0x00;
        THRID = 0x00;
        send();
        TYPE = temp;
    }

    /**
     * 舵机角度控制
     *
     * @param major 左侧舵机
     * @param first 右侧舵机
     */
    public void rudder_control(int major, int first) {
        byte temp = (byte) TYPE;
        TYPE = (short) 0x0C;
        MAJOR = (byte) 0x08;
        FIRST = (byte) major;
        SECOND = (byte) first;
        THRID = 0x00;
        send();
        TYPE = temp;
    }

    //立体车库控制
    public void garage_control(int type, int major, int first) {
        byte temp = (byte) TYPE;
        TYPE = (short) type;
        MAJOR = (byte) major;
        FIRST = (byte) first;
        SECOND = 0x00;
        THRID = 0x00;
        send();
        TYPE = temp;
    }

    public void gate(int major, int first, int second, int third) {// 闸门
        byte temp = (byte) TYPE;
        TYPE = 0x03;
        MAJOR = (byte) major;
        FIRST = (byte) first;
        SECOND = (byte) second;
        THRID = (byte) third;
        send();
        TYPE = temp;
    }

    //LCD 显示标志物进入计时模式
    public void digital_close() {//数码管关闭
        byte temp = (byte) TYPE;
        TYPE = 0x04;
        MAJOR = 0x03;
        FIRST = 0x00;
        SECOND = 0x00;
        THRID = 0x00;
        send();
        TYPE = temp;
    }

    public void digital_open() {//数码管打开
        byte temp = (byte) TYPE;
        TYPE = 0x04;
        MAJOR = 0x03;
        FIRST = 0x01;
        SECOND = 0x00;
        THRID = 0x00;
        send();
        TYPE = temp;
    }

    public void digital_clear() {//数码管清零
        byte temp = (byte) TYPE;
        TYPE = 0x04;
        MAJOR = 0x03;
        FIRST = 0x02;
        SECOND = 0x00;
        THRID = 0x00;
        send();
        TYPE = temp;
    }

    public void digital_dic(int dis) {//LCD显示标志物第二排显示距离

        byte temp = (byte) TYPE;
        int a = 0, b = 0, c = 0;
        a = (dis / 100) & (0xF);
        b = (dis % 100 / 10) & (0xF);
        c = (dis % 10) & (0xF);
        b = b << 4;
        b = b | c;
        TYPE = 0x04;
        MAJOR = 0x04;
        FIRST = 0x00;
        SECOND = (short) (a);
        THRID = (short) (b);
        send();
        TYPE = temp;
    }

    public void digital(int i, int one, int two, int three) {// 数码管
        byte temp = (byte) TYPE;
        TYPE = 0x04;
        if (i == 1) {//数据写入第一排数码管
            MAJOR = 0x01;
            FIRST = (byte) one;
            SECOND = (byte) two;
            THRID = (byte) three;
        } else if (i == 2) {//数据写入第二排数码管
            MAJOR = 0x02;
            FIRST = (byte) one;
            SECOND = (byte) two;
            THRID = (byte) three;
        }
        send();
        TYPE = temp;
    }

    public void VoiceBroadcast()  //语音播报随机指令
    {
        byte temp = (byte) TYPE;
        TYPE = (short) 0x06;
        MAJOR = (short) 0x20;
        FIRST = (byte) 0x01;
        SECOND = (byte) 0x00;
        THRID = (byte) 0x00;
        send();
        TYPE = temp;
    }

    public void TFT_LCD(int type, int MAIN, int KIND, int COMMAD, int DEPUTY)  //tft lcd
    {
        byte temp = (byte) TYPE;
        TYPE = (short) type;
        MAJOR = (short) MAIN;
        FIRST = (byte) KIND;
        SECOND = (byte) COMMAD;
        THRID = (byte) DEPUTY;
        send();
        TYPE = temp;
    }

    public void magnetic_suspension(int MAIN, int KIND, int COMMAD, int DEPUTY) //磁悬浮
    {
        byte temp = (byte) TYPE;
        TYPE = (short) 0x0A;
        MAJOR = (short) MAIN;
        FIRST = (byte) KIND;
        SECOND = (byte) COMMAD;
        THRID = (byte) DEPUTY;
        send();
        TYPE = temp;
    }

    // 沉睡
    public void yanchi(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
