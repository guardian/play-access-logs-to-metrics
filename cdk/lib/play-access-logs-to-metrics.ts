import type {GuStackProps} from "@guardian/cdk/lib/constructs/core";
import {GuStack} from "@guardian/cdk/lib/constructs/core";
import {App, Duration} from "aws-cdk-lib";
import {GuScheduledLambda} from "@guardian/cdk";
import {Schedule} from "aws-cdk-lib/aws-events";
import {Topic} from "aws-cdk-lib/aws-sns";
import {Runtime} from "aws-cdk-lib/aws-lambda";

const appName = "play-access-logs-to-metrics";

export class PlayAccessLogsToMetrics extends GuStack {
    constructor(scope: App, id: string, props: GuStackProps) {
        super(scope, id, props);

        const runDailyRule = {
            // 5am daily on weekdays
            schedule: Schedule.expression("cron(0 5 ? * MON-FRI *)"),
            description: "Daily run to process Play access logs and update metrics",
        };

        const snsTopic = new Topic(this, "AbTestingNotificationSnsTopic");

        new GuScheduledLambda(
            this,
            "PlayAccessLogsToMetricsLambda",
            {
                functionName: `${appName}-${props.stage}`,
                app: appName,
                fileName: "lambda.zip",
                handler: "com.gu.alb.Handler.handle",
                rules: this.stage === "PROD" ? [runDailyRule] : [],
                monitoringConfiguration: {
                    snsTopicName: snsTopic.topicName,
                    toleratedErrorPercentage: 0,
                    alarmName: `${appName}-${props.stage}-alarm`,
                    alarmDescription: `Something went wrong when processing access logs for ${appName}-${props.stage}. Please check the logs`,
                    lengthOfEvaluationPeriod: Duration.minutes(1),
                    numberOfEvaluationPeriodsAboveThresholdBeforeAlarm: 1,
                    datapointsToAlarm: 1,
                },
                runtime: Runtime.NODEJS_24_X,
                environment: {
                    STAGE: props.stage,
                },
            },
        );
    }
}
