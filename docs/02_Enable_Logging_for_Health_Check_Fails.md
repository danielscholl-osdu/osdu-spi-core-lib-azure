# Enable Logging For Health Check Failures

<ol>
<li>Ensure your service is running with SpringBoot version 2.4  or higher</li>
<li>Follow instructions from doc <a ref="docs/01_Using_Common_Properties.md">01_Using_Common_Properties.md </a>of core-lib-azure is implemented.<br/>
This is to enable actuator to run on a different port.
</li>
<li> Define Liveness and Readiness probes:
<ul> Add the following in your deployment.yaml file under 'containers' property:

```
readinessProbe:
    httpGet:
        path: /actuator/health
        port: 8081
livenessProbe:
    httpGet:
        path: /actuator/health
        port: 8081
    initialDelaySeconds: 250
    periodSeconds: 10
```
</ul>
</li>
<li>
Ensure that the application pom.xml refers to the latest core-lib version {0.10.0-rc11 & Upwards}
</li>
</ol>

For more details, refer to [this MR](https://community.opengroup.org/osdu/platform/security-and-compliance/legal/-/merge_requests/133)