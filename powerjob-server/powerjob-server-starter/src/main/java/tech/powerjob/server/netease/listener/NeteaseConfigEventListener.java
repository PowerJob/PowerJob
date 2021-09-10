package tech.powerjob.server.netease.listener;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author Echo009
 * @since 2021/9/9
 */
@SuppressWarnings("NullableProblems")
@Slf4j
public class NeteaseConfigEventListener implements GenericApplicationListener {

    private static final Class<?>[] EVENT_TYPES = {ApplicationEnvironmentPreparedEvent.class};

    private static final Class<?>[] SOURCE_TYPES = {SpringApplication.class};

    private static final int DEFAULT_ORDER = ConfigFileApplicationListener.DEFAULT_ORDER + 1;

    private static final String[] SEARCH_LOCATIONS = new String[]{
            "classpath:/netease/auth-%s.properties"
    };

    private static final String DEFAULT_PROPERTIES = "defaultProperties";


    public NeteaseConfigEventListener(){
        System.out.println(" ????? ");
    }


    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        return isAssignableFrom(resolvableType.getRawClass(), EVENT_TYPES);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return isAssignableFrom(sourceType, SOURCE_TYPES);
    }

    private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
        if (type != null) {
            for (Class<?> supportedType : supportedTypes) {
                if (supportedType.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            new Loader(((ApplicationEnvironmentPreparedEvent) event).getEnvironment()).load();

        }
    }


    private static class Loader {
        private final ConfigurableEnvironment environment;

        public Loader(ConfigurableEnvironment environment) {
            this.environment = environment;
        }

        public void load() {
            try {
                PropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader();

                ResourcePatternResolver resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(null);

                List<PropertySource<?>> propertySourceList = new LinkedList<>();
                List<Resource> resourceList = new LinkedList<>();


                String[] profiles = environment.getActiveProfiles();
                for (String profile : profiles) {
                    for (String location : SEARCH_LOCATIONS) {
                        loadProperties(propertiesPropertySourceLoader, resourcePatternResolver, propertySourceList, resourceList, profile, location);
                    }
                }

                addConfigurationProperties(propertySourceList);
                log.info("[module properties loaded, resource:{}]", resourceList);
            } catch (Exception e) {
                log.error("fail to load module properties", e);
            }
        }

        private void loadProperties(PropertySourceLoader propertiesPropertySourceLoader, ResourcePatternResolver resourcePatternResolver, List<PropertySource<?>> propertySourceList, List<Resource> resourceList, String profile, String location) {
            try {
                Resource[] resources = resourcePatternResolver.getResources(String.format(location, profile));
                log.info("location:{},profile:{},resources:{}", location, profile, Arrays.toString(resources));
                for (Resource resource : resources) {
                    if (resource != null && resource.exists()) {
                        String group = "module-" + resource.getURL().toString().hashCode();
                        List<PropertySource<?>> propertySources = propertiesPropertySourceLoader.load(group, resource);
                        if (propertySources != null && !propertySources.isEmpty()) {
                            propertySourceList.addAll(0, propertySources);
                            resourceList.add(0, resource);
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Getting resources {} error for reason: {} ", String.format(location, profile),
                        e.getMessage());
            }
        }

        private void addConfigurationProperties(List<PropertySource<?>> reorderedSources) {
            addConfigurationProperties(new ModuleConfigurationPropertySource(reorderedSources));
        }

        private void addConfigurationProperties(ModuleConfigurationPropertySource configurationSources) {
            MutablePropertySources existingSources = this.environment.getPropertySources();
            if (existingSources.contains(DEFAULT_PROPERTIES)) {
                existingSources.addBefore(DEFAULT_PROPERTIES, configurationSources);
            } else {
                existingSources.addLast(configurationSources);
            }
        }


    }


    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }


    @EqualsAndHashCode(of = {"sources", "names"}, callSuper = true)
    static class ModuleConfigurationPropertySource extends EnumerablePropertySource<Collection<PropertySource<?>>> {

        private final Collection<PropertySource<?>> sources;

        private final String[] names;

        ModuleConfigurationPropertySource(Collection<PropertySource<?>> sources) {
            super("moduleConfigurationProperties", sources);
            this.sources = sources;
            List<String> nameList = new ArrayList<>();
            for (PropertySource<?> source : sources) {
                if (source instanceof EnumerablePropertySource) {
                    nameList.addAll(Arrays.asList(((EnumerablePropertySource<?>) source).getPropertyNames()));
                }
            }
            this.names = nameList.toArray(new String[0]);
        }

        @Override
        public Object getProperty(String name) {
            for (PropertySource<?> propertySource : this.sources) {
                Object value = propertySource.getProperty(name);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public String[] getPropertyNames() {
            return this.names;
        }
    }
}
