#!groovy

def decidePipeline(Map configMap)
{
    application = configMap.get("application")
    switch(application) {
        case 'nodejsvm':
            nodejsvm(configMap)    
            break
    }
}