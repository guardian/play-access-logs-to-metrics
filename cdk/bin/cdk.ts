import "source-map-support/register";
import { GuRoot } from "@guardian/cdk/lib/constructs/root";
import { PlayAccessLogsToMetrics } from "../lib/play-access-logs-to-metrics";

const app = new GuRoot();
new PlayAccessLogsToMetrics(app, "PlayAccessLogsToMetrics-euwest-1-CODE", { stack: "frontend", stage: "CODE", env: { region: "eu-west-1" } });
new PlayAccessLogsToMetrics(app, "PlayAccessLogsToMetrics-euwest-1-PROD", { stack: "frontend", stage: "PROD", env: { region: "eu-west-1" } });
