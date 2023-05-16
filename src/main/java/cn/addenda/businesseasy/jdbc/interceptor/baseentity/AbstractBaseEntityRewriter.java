package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

/**
 * @author addenda
 * @since 2023/5/14 16:40
 */
public abstract class AbstractBaseEntityRewriter implements BaseEntityRewriter {

    protected final BaseEntitySource baseEntitySource;

    protected AbstractBaseEntityRewriter(BaseEntitySource baseEntitySource) {
        this.baseEntitySource = baseEntitySource;
    }
}
