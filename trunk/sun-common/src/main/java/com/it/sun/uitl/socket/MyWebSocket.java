package com.it.sun.uitl.socket;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;


@ServerEndpoint(value = "/websocket")
@Component
public class MyWebSocket {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();

    private static Map<String,List<Session>> sessMap=new HashMap<String,List<Session>>();//将 会话 分类，按照 openid分组，openid是前端生成的

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        try {
//            sendMessage(CommonConstant.CURRENT_WANGING_NUMBER.toString());
            sendMessage("连接websocket成功");
        } catch (IOException e) {
            System.out.println("IO异常");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);
        try {
            //开始分析数据
            JSONObject jo = JSONObject.parseObject(message);
            String openid=jo.getString("openid");//
            String msg=jo.getString("msg");//
            jo.put("c",200);

            List<Session> sessS;
            //判断openid是否存在sessMap中
            if(sessMap.containsKey(openid)){//存在就判断当前session是否在map的list中
                sessS=sessMap.get(openid);

                boolean fl=true;
                for (int i=0;i<sessS.size();i++){
                    if(sessS.get(i).getId().equals(session.getId())){
                        fl=false;
                        break;
                    }
                }
                if(fl){
                    sessS.add(session);
                }
            }else {//不存在就创建
                sessS=new ArrayList<Session>();
                sessS.add(session);
                sessMap.put(openid,sessS);
            }

            //群发消息
            for (int i=0;i<sessS.size();i++) {
                try {
                    sessS.get(i).getBasicRemote().sendText(jo.toJSONString());
                } catch (Exception e) {
                    sessS.remove(i);
                    i--;
                }
            }
        }catch (Exception e){
            try {
                JSONObject resultJo=new JSONObject();
                resultJo.put("c", 500);
                resultJo.put("msg","消息格式不正确");
                session.getBasicRemote().sendText(resultJo.toJSONString());
            }catch (Exception epx){}
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }


    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }


    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        MyWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        MyWebSocket.onlineCount--;
    }
}
