package com.example.blog.web.listener;

import com.example.blog.bean.User;
import com.example.blog.util.Const;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在线人数监听器（HttpSessionListener + HttpSessionAttributeListener）。
 *
 * <p>两个计数不是一回事，这点最容易讲错：</p>
 * <ul>
 *   <li><b>在线访客</b>：只要有 Session 就算（游客访问首页也会创建 Session），
 *       对应 sessionCreated / sessionDestroyed；</li>
 *   <li><b>在线会员</b>：Session 里出现 loginUser 才算，对应 attributeAdded / attributeRemoved。</li>
 * </ul>
 *
 * <p>为什么用 CopyOnWriteArrayList？监听器方法由容器的多个请求线程并发调用，
 * 普通 ArrayList 在高并发下会抛 ConcurrentModificationException。
 * 这里读多写少，CopyOnWrite 正合适。</p>
 */
@WebListener
public class OnlineUserListener implements HttpSessionListener, HttpSessionAttributeListener {

    private static final CopyOnWriteArrayList<String> LOGGED_USERS = new CopyOnWriteArrayList<String>();
    private static final String KEY_ONLINE_GUEST = OnlineUserListener.class.getName() + ".guest";

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        ServletContext ctx = event.getSession().getServletContext();
        Object value = ctx.getAttribute(Const.CONTEXT_ONLINE);
        if (value instanceof AtomicInteger) {
            ((AtomicInteger) value).incrementAndGet();
        }
        event.getSession().setAttribute(KEY_ONLINE_GUEST, Boolean.TRUE);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        ServletContext ctx = event.getSession().getServletContext();
        Object value = ctx.getAttribute(Const.CONTEXT_ONLINE);
        if (value instanceof AtomicInteger) {
            AtomicInteger counter = (AtomicInteger) value;
            counter.updateAndGet(v -> Math.max(v - 1, 0));
        }
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if (Const.SESSION_USER.equals(event.getName()) && event.getValue() instanceof User) {
            User user = (User) event.getValue();
            if (!LOGGED_USERS.contains(user.getDisplayName())) {
                LOGGED_USERS.add(user.getDisplayName());
            }
            event.getSession().getServletContext().setAttribute("loggedUsers", LOGGED_USERS);
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if (Const.SESSION_USER.equals(event.getName()) && event.getValue() instanceof User) {
            User user = (User) event.getValue();
            LOGGED_USERS.remove(user.getDisplayName());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        if (Const.SESSION_USER.equals(event.getName())) {
            attributeRemoved(event);
            attributeAdded(event);
        }
    }
}