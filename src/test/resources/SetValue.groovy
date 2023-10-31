node {
    def result = pmanSetValueText(dryRun: true, valueName: 'test.value', apiKey: "34f117f1-abd1-4e43-b2d3-ae43ee060862:secretsecret", value: "123")
    echo "Result is ${result}"
}