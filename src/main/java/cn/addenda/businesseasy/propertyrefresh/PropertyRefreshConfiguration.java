package cn.addenda.businesseasy.propertyrefresh;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @Author ISJINHAO
 * @Date 2022/4/2 20:32
 */
@Configuration
public class PropertyRefreshConfiguration implements ImportAware {

    protected AnnotationAttributes annotationAttributes;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.annotationAttributes = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnablePropertyRefresh.class.getName(), false));
        if (this.annotationAttributes == null) {
            throw new IllegalArgumentException(
                    "@EnablePropertyRefresh is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Bean
    public PropertyRefreshBeanPostProcessor propertyRefreshPostProcessor() {
        PropertyRefreshBeanPostProcessor postProcessor = new PropertyRefreshBeanPostProcessor();
        postProcessor.setOrder(annotationAttributes.getNumber("order"));
        postProcessor.setThreadSizes(annotationAttributes.getNumber("threadSizes"));
        return postProcessor;
    }

}
