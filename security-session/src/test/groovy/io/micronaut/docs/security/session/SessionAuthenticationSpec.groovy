
package io.micronaut.docs.security.session

import geb.spock.GebSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.cookie.Cookie
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.handlers.RejectionHandler
import io.micronaut.security.session.SessionSecurityfilterRejectionHandler
import io.micronaut.testutils.YamlAsciidocTagCleaner
import org.yaml.snakeyaml.Yaml
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

class SessionAuthenticationSpec extends GebSpec implements YamlAsciidocTagCleaner {

    String yamlConfig = '''\
//tag::yamlconfig[]
micronaut:
  security:
    enabled: true
    endpoints:
      login:
        enabled: true
      logout:
        enabled: true
    session:
      enabled: true
      login-failure-target-url: /login/authFailed
'''//end::yamlconfig[]

    @Shared
    Map<String, Object> configMap = ['micronaut': [
            'security': [
                    'enabled': true,
                    'endpoints': [
                            'login': [
                                    'enabled': true,
                            ],
                            'logout': [
                                    'enabled': true,
                            ],
                    ],
                    'session': [
                            'enabled': true,
                            'login-failure-target-url': '/login/authFailed',
                    ]
            ]
        ]
    ]

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run([
                    'spec.name': 'securitysession',
                    'micronaut.http.client.followRedirects': false
            ] << flatten(configMap), Environment.TEST)

    @Shared
    EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    @Shared
    @AutoCleanup
    RxHttpClient client = embeddedServer.applicationContext.createBean(RxHttpClient, embeddedServer.getURL())

    def "verify session based authentication works"() {
        given:
        browser.baseUrl = "http://localhost:${embeddedServer.port}"

        when:
        to HomePage

        then:
        at HomePage

        when:
        HomePage homePage = browser.page HomePage

        then:
        homePage.username() == null

        when:
        homePage.login()

        then:
        at LoginPage

        when:
        LoginPage loginPage = browser.page LoginPage
        loginPage.login('foo', 'foo')

        then:
        at LoginPage

        and:
        loginPage.hasErrors()

        when:
        loginPage.login('sherlock', 'password')

        then:
        at HomePage

        when:
        homePage = browser.page HomePage

        then:
        homePage.username() == 'sherlock'

        when:
        homePage.logout()

        then:
        at HomePage

        when:
        homePage = browser.page HomePage

        then:
        homePage.username() == null
    }

    def "verify session based authentication works without a real browser"() {
        given:
        context.getBean(HomeController.class)
        context.getBean(LoginAuthController.class)
        context.getBean(AuthenticationProviderUserPassword.class)

        when:
        Map m = new Yaml().load(cleanYamlAsciidocTag(yamlConfig))

        then:
        m == configMap

        when:
        HttpRequest request = HttpRequest.GET('/')
        HttpResponse<String> rsp = client.toBlocking().exchange(request, String)

        then:
        rsp.status().code == 200
        rsp.body()
        rsp.body().contains('You are not logged in')

        when:
        HttpRequest loginRequest = HttpRequest.POST('/login', new LoginForm(username: 'foo', password: 'foo'))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)

        HttpResponse<String> loginRsp = client.toBlocking().exchange(loginRequest, String)

        then:
        loginRsp.status().code == 303

        and: 'login fails, cookie is not set'
        !loginRsp.getHeaders().get('Set-Cookie')

        when:
        loginRequest = HttpRequest.POST('/login', new LoginForm(username: 'sherlock', password: 'password'))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)

        loginRsp = client.toBlocking().exchange(loginRequest, String)

        then:
        loginRsp.status().code == 303

        when:
        String cookie = loginRsp.getHeaders().get('Set-Cookie')
        println cookie
        then:
        cookie
        cookie.contains('SESSION=')
        cookie.endsWith('; HTTPOnly')

        when:
        String sessionId = cookie.replaceAll('SESSION=', '').replaceAll('; HTTPOnly', '')
        request = HttpRequest.GET('/').cookie(Cookie.of('SESSION', sessionId))
        rsp = client.toBlocking().exchange(request, String)

        then:
        rsp.status().code == 200
        rsp.body()
        rsp.body().contains('sherlock')
    }

    def "verifies default RejectionHandler is SessionSecurityfilterRejectionHandler"() {
        when:
        RejectionHandler rejectionHandler = context.getBean(RejectionHandler)

        then:
        noExceptionThrown()
        rejectionHandler instanceof SessionSecurityfilterRejectionHandler
    }
}
