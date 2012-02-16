/*
 * Copyright 2008-2012 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.torquebox.core.component;

import java.util.Map;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.msc.inject.Injector;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.torquebox.core.injection.analysis.Injectable;
import org.torquebox.core.util.RuntimeHelper;

public class ComponentResolver {
    
    public static AttachmentKey<AttachmentList<Injectable>> ADDITIONAL_INJECTABLES = AttachmentKey.createList( Injectable.class );

    public ComponentResolver(boolean alwaysReload) {
    	this.alwaysReload = alwaysReload;
    }

    public RubyComponent resolve(final Ruby runtime) throws Exception {
        final ComponentRegistry registry = ComponentRegistry.getRegistryFor( runtime );
        IRubyObject rubyComponent = null;

        if (!this.alwaysReload) {
            rubyComponent = registry.lookup( this.componentName );
        } else {
        	// not yet sure this is needed - reloading is broken with and without the next two lines
        	RuntimeHelper.evalScriptlet( runtime, "Dispatcher.cleanup_application if defined?(Dispatcher) && Dispatcher.respond_to?(:cleanup_application)" ); // rails2
        	RuntimeHelper.evalScriptlet( runtime, "ActiveSupport::Dependencies.clear if defined?(ActiveSupport::Dependencies) && ActiveSupport::Dependencies.respond_to?(:clear)" ); // rails3
        }

        if (rubyComponent == null) {
            rubyComponent = createComponent( runtime );
            registry.register( this.componentName, rubyComponent );
        }

        if (rubyComponent == null) {
            return null;
        }

        return wrapComponent( rubyComponent );
    }

    protected synchronized IRubyObject createComponent(final Ruby runtime) throws Exception {
        prepareInjections(runtime);
        IRubyObject rubyComponent = this.componentInstantiator.newInstance( runtime, this.initializeParams );
        return rubyComponent;
    }

    protected void prepareInjections(final Ruby runtime) throws Exception {
        this.injectionRegistry.merge( runtime );
    }
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return this.componentName;
    }

    public void setComponentInstantiator(ComponentInstantiator componentInstantiator) {
        this.componentInstantiator = componentInstantiator;
    }

    public ComponentInstantiator getComponentInstantiator() {
        return this.componentInstantiator;
    }

    public void setInitializeParams(Object[] initializeParams) {
        this.initializeParams = initializeParams;
    }

    @SuppressWarnings("rawtypes")
    public void setInitializeParams(Map params) {
        if (params != null) {
            setInitializeParams( new Object[] { params } );
        }
    }

    public Object[] getInitializeParams() {
        return this.initializeParams;
    }
    
    public void setAlwaysReload(boolean alwaysReload) {
        this.alwaysReload = alwaysReload;
    }
    
    public boolean isAlwaysReload() {
        return this.alwaysReload;
    }

    public void setComponentWrapperClass(Class<? extends AbstractRubyComponent> wrapperClass) {
        this.wrapperClass = wrapperClass;
    }

    public Class<? extends AbstractRubyComponent> getComponentWrapperClass() {
        return this.wrapperClass;
    }
    
    public void setComponentWrapperOptions(Map<String,Object> componentWrapperOptions) {
        this.componentWrapperOptions = componentWrapperOptions;
    }
    
    public Map<String,Object> getComponentWrapperOptions() {
        return this.componentWrapperOptions;
    }

    protected RubyComponent wrapComponent(IRubyObject rubyComponent) throws InstantiationException, IllegalAccessException {
        AbstractRubyComponent wrappedComponent = this.wrapperClass.newInstance();
        wrappedComponent.setOptions( this.componentWrapperOptions );
        wrappedComponent.setRubyComponent( rubyComponent );
        wrappedComponent.setNamespaceContextSelector( this.namespaceContextSelector );
        return wrappedComponent;
    }
    
    public void setNamespaceContextSelector(NamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }

    public Injector<Object> getInjector(String key) {
        return this.injectionRegistry.getInjector( key );
    }

    private Class<? extends AbstractRubyComponent> wrapperClass = AbstractRubyComponent.class;
    private Map<String, Object> componentWrapperOptions;

    private NamespaceContextSelector namespaceContextSelector;
    private InjectionRegistry injectionRegistry = new InjectionRegistry();
    private ComponentInstantiator componentInstantiator;
    private String componentName;
    private Object[] initializeParams;
    private boolean alwaysReload = false;

}
