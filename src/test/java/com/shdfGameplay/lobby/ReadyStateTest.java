package com.shdfGameplay.lobby;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReadyState 准备账本离线测试(纯状态机零服务器):记/查/清单个标记、allReady 各口径
 * (空集=未就绪/全员 ready 才 true/who 含未记入者 false)、retainPresent 清离场者记录、
 * clearAll 全清(周期停止与开局失败节流两调用口径的账本面)。
 * 出自 2.5 大厅逻辑(04-大厅模式 §4.2、§4.3;2.5 步骤4)。
 */
class ReadyStateTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID U3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void 记入与查态往返() {
        final ReadyState ready = new ReadyState();

        assertFalse(ready.isReady(U1), "未记入视为未准备");
        ready.setReady(U1, true);
        assertTrue(ready.isReady(U1), "记入后已准备");
        ready.setReady(U1, false);
        assertFalse(ready.isReady(U1), "ready=false 清标记");
    }

    @Test
    void allReady_空集视为未就绪() {
        final ReadyState ready = new ReadyState();
        ready.setReady(U1, true);

        assertFalse(ready.allReady(List.of()), "空=未就绪(人数门槛仍住开局门)");
    }

    @Test
    void allReady_全员ready才true() {
        final ReadyState ready = new ReadyState();
        ready.setReady(U1, true);
        ready.setReady(U2, true);

        assertTrue(ready.allReady(List.of(U1, U2)), "全员已记 ready");
        ready.setReady(U2, false);
        assertFalse(ready.allReady(List.of(U1, U2)), "部分未 ready:false");
        assertFalse(ready.allReady(List.of(U1, U3)), "who 含未记入者(如已离场):false");
    }

    @Test
    void retainPresent_清不在场者记录() {
        final ReadyState ready = new ReadyState();
        ready.setReady(U1, true);
        ready.setReady(U2, true);

        ready.retainPresent(List.of(U1));
        assertTrue(ready.isReady(U1), "在场者标记保留");
        assertFalse(ready.isReady(U2), "不在场者记录被清");
        assertTrue(ready.allReady(List.of(U1)), "在场者 ready 判定不受影响");
        assertFalse(ready.allReady(List.of(U1, U2)), "含被清者:false");
    }

    @Test
    void clearAll_全清() {
        final ReadyState ready = new ReadyState();
        ready.setReady(U1, true);
        ready.setReady(U2, true);

        ready.clearAll();
        assertFalse(ready.isReady(U1));
        assertFalse(ready.isReady(U2));
        assertFalse(ready.allReady(List.of(U1)), "全清后任何群体判定均未就绪");
        assertFalse(ready.allReady(List.of(U1, U2, U3)));
    }
}
