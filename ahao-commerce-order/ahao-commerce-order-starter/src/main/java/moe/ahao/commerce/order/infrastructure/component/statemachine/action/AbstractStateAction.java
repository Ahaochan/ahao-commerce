package moe.ahao.commerce.order.infrastructure.component.statemachine.action;

/**
 * T 表示入参，R表示出参
 */
public abstract class AbstractStateAction<T, R, E> implements StateAction<E> {

    @Override
    public void onStateChange(E event, Object context) {
        R r = this.onStateChangeInternal(event, (T) context);
        this.postStateChange(event, r);
    }

    /**
     * 状态变更操作
     *
     * @param event   事件
     * @param context 上下文
     * @return 返回标准的数据，正向是OrderInfoDTO, 逆向是AfterSaleStateMachineDTO
     */
    protected abstract R onStateChangeInternal(E event, T context);

    /**
     * 状态变更后置操作
     *
     * @param event   事件
     * @param context 上下文
     */
    protected void postStateChange(E event, R context) {
        // 默认空实现, 提供给子类一个钩子
    }
}
