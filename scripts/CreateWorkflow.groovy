includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target('default': "Creates a new metridoc workflow") {
    depends(checkVersion, parseArguments)

    def type = "Workflow"
    promptForName(type: type)

    def name = argsMap["params"][0]
    createArtifact(name: name, suffix: type, type: type, path: "grails-app/workflows")
}
