Development containers: <br />
<h3>Postgressql</h3>
docker run -d --name postgresql -p 5432:5432 -e "POSTGRES_PASSWORD=root" postgres <br />
<h3>Docker builds</h3>
docker build . -t [image] -f src/main/Dockerfile<br />
<h3>Docker runs</h3>
docker run -d --name test -p 8080:8080 -v [localPath]:/app/application.properties [image]<br />
<br />
<b>Environment variables:</b>
- $CONFIG_LOCATION = file:/app/application.properties (default)

<h3>Todo:</h3>
- frontend - move http calls to service
- backend - create token refresh ESI call
- frontend - add reauthentication popup for main character and children