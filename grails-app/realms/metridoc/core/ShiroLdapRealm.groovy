package metridoc.core

import org.apache.shiro.authc.AccountException
import org.apache.shiro.authc.CredentialsException
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.UnknownAccountException

import javax.naming.AuthenticationException
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls

/**
 * Simple realm that authenticates users against an LDAP server.
 */
class ShiroLdapRealm {
    static authTokenClass = org.apache.shiro.authc.UsernamePasswordToken
    static LOCALHOST_LDAP = "ldap://localhost:389/"
    def grailsApplication

    def authenticate(authToken) {
        log.info "Attempting to authenticate ${authToken.username} in LDAP realm..."
        def username = authToken.username
        def password = new String(authToken.password)


        def appConfig = LdapData.list().get(0)
        def ldapUrls = appConfig.server ?: [LOCALHOST_LDAP]
        def searchBase = appConfig.rootDN
        def searchUser = appConfig.userSearchBase
        def searchPass = appConfig.unencryptedPassword
        def searchScope = 2
        def usernameAttribute = appConfig.userSearchFilter
        def skipAuthc = appConfig.skipAuthentication
        def skipCredChk = appConfig.skipCredentialsCheck
        def allowEmptyPass = appConfig.allowEmptyPasswords

        // Skip authentication ?
        if (skipAuthc) {
            log.info "Skipping authentication in development mode."
            return username
        }

        // Null username is invalid
        if (username == null) {
            throw new AccountException("Null usernames are not allowed by this realm.")
        }

        // Empty username is invalid
        if (username == "") {
            throw new AccountException("Empty usernames are not allowed by this realm.")
        }

        // Allow empty passwords ?
        if (!allowEmptyPass) {
            // Null password is invalid
            if (password == null) {
                throw new CredentialsException("Null password are not allowed by this realm.")
            }

            // empty password is invalid
            if (password == "") {
                throw new CredentialsException("Empty passwords are not allowed by this realm.")
            }
        }

        // Accept strings and GStrings for convenience, but convert to
        // a list.
        if (ldapUrls && !(ldapUrls instanceof Collection)) {
            ldapUrls = [ldapUrls]
        }

        // Set up the configuration for the LDAP search we are about
        // to do.
        def env = new Hashtable()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        if (searchUser) {
            // Non-anonymous access for the search.
            env[Context.SECURITY_AUTHENTICATION] = "simple"
            env[Context.SECURITY_PRINCIPAL] = searchUser
            env[Context.SECURITY_CREDENTIALS] = searchPass
        }

        // Find an LDAP server that we can connect to.
        def ctx
        def urlUsed = ldapUrls.find { url ->
            log.info "Trying LDAP server ${url} ..."
            env[Context.PROVIDER_URL] = url

            // If an exception occurs, log it.
            try {
                ctx = new InitialDirContext(env)
                return true
            }
            catch (NamingException e) {
                if (url != LOCALHOST_LDAP) {
                    log.error "Could not connect to ${url}: ${e}"
                }
                return false
            }
        }

        if (!urlUsed) {
            def msg = 'No LDAP server available.'
            log.warn msg
            throw new org.apache.shiro.authc.AuthenticationException(msg)
        }

        // Look up the DN for the LDAP entry that has a 'uid' value
        // matching the given username.
        SearchControls searchControls = new SearchControls()
        searchControls.setSearchScope(searchScope)
        String filter = "($usernameAttribute=$username)"

        def result = ctx.search(searchBase, filter, searchControls)
        if (!result.hasMore()) {
            throw new UnknownAccountException("No account found for user [${username}]")
        }

        // Skip credentials check ?
        if (skipCredChk) {
            log.info "Skipping credentials check in development mode."
            return username
        }

        // Now connect to the LDAP server again, but this time use
        // authentication with the principal associated with the given
        // username.
        def searchResult = result.next()
        env[Context.SECURITY_AUTHENTICATION] = "simple"
        env[Context.SECURITY_PRINCIPAL] = searchResult.nameInNamespace
        env[Context.SECURITY_CREDENTIALS] = password

        try {
            new InitialDirContext(env)
            return username
        }
        catch (AuthenticationException ex) {
            log.info "Invalid password"
            throw new IncorrectCredentialsException("Invalid password for user '${username}'")
        }
    }
}
