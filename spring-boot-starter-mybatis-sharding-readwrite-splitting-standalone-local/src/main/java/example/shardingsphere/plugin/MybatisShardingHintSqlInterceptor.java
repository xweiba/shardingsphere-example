package example.shardingsphere.plugin;

import example.shardingsphere.annotation.ShadingHintParam;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class MybatisShardingHintSqlInterceptor implements Interceptor {

    @Value("${mybatis.interceptor.hint.targetTables:}")
    private List<String> targetTables; // 从配置中获取需要添加注释的表名列表

    @Value("${mybatis.interceptor.hint.defaultValue:1}")
    private Integer defaultParamValue; // 从配置中获取需要添加注释的表名列表

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

        // 解析参数
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        // 获取 @ShadingHintParam 注解在第几个参数
        Integer shadingHintParamValueIndex = getShadingHintParamValueIndex(metaObject);

        // 只支持单表查询
        for (String targetTable : targetTables) {
            // 获取当前执行的SQL
            BoundSql boundSql = statementHandler.getBoundSql();
            String originalSql = boundSql.getSql();

            if (originalSql.contains(targetTable)) {
                String comment = getHintSql(shadingHintParamValueIndex, statementHandler);
                String newSql = comment + " " + originalSql;
                metaObject.setValue("delegate.boundSql.sql", newSql);
                break;
            }
        }

        // 继续执行下一个拦截器或者调用原方法
        return invocation.proceed();
    }

    private Integer getShadingHintParamValueIndex(MetaObject metaObject) throws ClassNotFoundException {
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        Class<?> mapperInterface = Class.forName(mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf(".")));
        String methodName = mappedStatement.getId().substring(mappedStatement.getId().lastIndexOf(".") + 1);
        Method method = findMethod(mapperInterface, methodName);
        return getShadingHintParamValueIndex(method);
    }

    private Method findMethod(Class<?> mapperInterface, String methodName) {
        for (Method method : mapperInterface.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Cannot find method " + methodName + " in Mapper interface " + mapperInterface);
    }

    private Integer getShadingHintParamValueIndex(Method method) {
        Integer result = null;
        if (method != null) {
            int parameterCount = method.getParameterCount();
            if (parameterCount > 0) {
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                // 遍历参数注解获取 @Param 注解的参数名称
                for (int i = 0; i < parameterAnnotations.length; i++) {
                    Annotation[] annotations = parameterAnnotations[i];
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType() == ShadingHintParam.class) {
                            result = i;
                        }
                    }
                }
            }
        }

        return result;
    }



    private String getHintSql(Integer shadingHintParamValueIndex, StatementHandler statementHandler) {
        Object shadingHintParamValue = null;
        if (shadingHintParamValueIndex != null) {
            if (shadingHintParamValueIndex == 0) {
                shadingHintParamValue = statementHandler.getParameterHandler().getParameterObject();
            } else {
                MapperMethod.ParamMap<Object> parameterMap = (MapperMethod.ParamMap) statementHandler.getParameterHandler().getParameterObject();
                for (Map.Entry<String, Object> stringObjectEntry : parameterMap.entrySet()) {
                    if (stringObjectEntry.getKey().endsWith(String.valueOf(shadingHintParamValueIndex))) {
                        shadingHintParamValue = stringObjectEntry.getValue();
                        break;
                    }
                }
            }
        }

        if (shadingHintParamValue == null) {
            shadingHintParamValue = defaultParamValue;
        }
        // 添加注释到SQL前
        return "/* SHARDINGSPHERE_HINT: SHARDING_DATABASE_VALUE=" + shadingHintParamValue + "*/";
    }
}
