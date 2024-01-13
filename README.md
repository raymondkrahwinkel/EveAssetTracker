Disclaimer: This is my first angular + java application ;) <br />
<br />
Development containers:
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
- add unit tests for EsiWalletService
- add switch system between characters (backend + frontend)
- create overview to show the wallet history per day (table date,startValue,value,difference)
- create system to download your assets and create estimated character value
- create view to search through your assets
- add run recording, create a snapshot of the current wallet value and calculate difference constantly until the recording is stopped (include ISK per hour)
- add setting to enable run/snapshot cleanup + interval days settings

* system to record your runs (like abysstracker before/after, only then for missions, deds, mining or other stuff you want to record)