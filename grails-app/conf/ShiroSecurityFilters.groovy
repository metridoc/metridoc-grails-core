/*
 * Copyright 2010 Trustees of the University of Pennsylvania Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken

/**
 * Generated by the Shiro plugin. This filters class protects all URLs
 * via access control by convention.
 */
class ShiroSecurityFilters {
    //this forces spring to initialize, otherwise this filter will prematurely access the application context before
    //it has been initialized and throw an exception
    def grailsApplication
    def securityService
    def anonymousApps
    Closure fallback
    def customSecurityByController = [:]
    private initiated = false

    def filters = {
        all(uri: "/**") {
            before = {

//                if (!initiated) {
//                    def security = grailsApplication.config.metridoc.security
//                    anonymousApps = security.anonymous
//                    fallback = security.fallback
//                    if (security.custom) {
//                        security.custom.each {
//                            customSecurityByController.put(it.key, it.value)
//                        }
//                    }
//
//                    initiated = true
//                }
//                //if we are not logged in, let's login as anonymous
//                if (!SecurityUtils.subject.isAuthenticated()) {
//                    log.info "currently not logged in, logging in as anonymous user"
//                    def authToken = new UsernamePasswordToken("anonymous", "password")
//                    SecurityUtils.subject.login(authToken)
//                }
//
//                // Ignore direct views (e.g. the default main index page).
//                if (!controllerName) return true
//
//                accessControl {
//
//                    if (role("ROLE_ADMIN")) return true //role admin has access to everything
//
//                    def runClosure = {closure ->
//                        def accessDelegate = new MetridocFilterAccessControlBuilder(subject)
//                        accessDelegate.request = request
//                        accessDelegate.response = response
//                        accessDelegate.session = session
//                        accessDelegate.servletContext = servletContext
//                        accessDelegate.flash = flash
//                        accessDelegate.params = params
//                        accessDelegate.actionName = actionName
//                        accessDelegate.controllerName = controllerName
//                        accessDelegate.grailsApplication = grailsApplication
//                        accessDelegate.applicationContext = applicationContext
//
//                        closure.delegate = accessDelegate
//                        fallback.resolveStrategy = Closure.DELEGATE_FIRST
//                        closure.call()
//                    }
//
//                    if(customSecurityByController[controllerName]) {
//                        def controllerSecurity = customSecurityByController[controllerName]
//                        return runClosure(controllerSecurity)
//                    }
//
//                    if (anonymousApps.contains(controllerName)) return true
//
//                    return runClosure(fallback)
//                }

                //if we are not logged in, let's login as anonymous
                if (!SecurityUtils.subject.isAuthenticated()) {
                    log.info "currently not logged in, logging in as anonymous user"
                    def authToken = new UsernamePasswordToken("anonymous", "password")
                    SecurityUtils.subject.login(authToken)
                }

                if (!controllerName) return true

                accessControl {
                    securityService.authorized(
                        [
                            request: request,
                            response: response,
                            session: session,
                            servletContext: servletContext,
                            flash: flash,
                            params: params,
                            actionName: actionName,
                            controllerName: controllerName,
                            grailsApplication: grailsApplication,
                            applicationContext: applicationContext
                        ]
                        , controllerName
                    )
                }
            }
        }
    }
}
