import {GuParameter, GuStack, GuStackProps} from "@guardian/cdk/lib/constructs/core";
import {App, Duration} from "aws-cdk-lib";
import {GuScheduledLambda} from "@guardian/cdk";
import {Schedule} from "aws-cdk-lib/aws-events";
import {Topic} from "aws-cdk-lib/aws-sns";
import {Runtime} from "aws-cdk-lib/aws-lambda";
import {Effect, PolicyStatement} from "aws-cdk-lib/aws-iam";

const appName = "play-access-logs-to-metrics";

export class PlayAccessLogsToMetrics extends GuStack {
    constructor(scope: App, id: string, props: GuStackProps) {
        super(scope, id, props);

        const playAccessLogsConfigParam = new GuParameter(
            this,
            'playAccessLogsConfigParam',
            {
                default: `/${this.stage}/${this.stack}/${appName}/inputConfig`,
                fromSSM: true,
                type: 'String',
                description:
                    'The JSON encoded configuration, containing which route file for which app to process',
            },
        );

        const athenaOutputBucketParam = new GuParameter(
            this,
            'athenaOutputBucketParam',
            {
                default: `/${this.stage}/${this.stack}/${appName}/athenaOutputBucket`,
                fromSSM: true,
                type: 'String',
                description:
                    'The S3 location where Athena query results should be stored, e.g. s3://aws-frontend-logs/athena-output/',
            },
        );

        const runDailyRule = {
            // 5am daily on weekdays
            schedule: Schedule.expression("cron(0 5 ? * MON-FRI *)"),
            description: "Daily run to process Play access logs and update metrics",
        };

        const snsTopic = new Topic(this, "AbTestingNotificationSnsTopic");

        const lambdaPolicies = [
            new PolicyStatement({
                sid: "AthenaQueryExecution",
                effect: Effect.ALLOW,
                actions: [
                    "athena:StartQueryExecution",
                    "athena:GetQueryExecution",
                    "athena:GetQueryResults",
                ],
                resources: [
                    `arn:aws:athena:eu-west-1:${this.account}:workgroup/primary`,
                ],
            }),
            new PolicyStatement({
                sid: "GlueCatalogAccess",
                effect: Effect.ALLOW,
                actions: [
                    "glue:GetDatabase",
                    "glue:GetTable",
                    "glue:GetPartitions",
                ],
                resources: [
                    `arn:aws:glue:eu-west-1:${this.account}:catalog`,
                    `arn:aws:glue:eu-west-1:${this.account}:database/gucdk_access_logs`,
                    `arn:aws:glue:eu-west-1:${this.account}:table/gucdk_access_logs/*`,
                ],
            }),
            new PolicyStatement({
                sid: "S3OutputLocation",
                effect: Effect.ALLOW,
                actions: [
                    "s3:GetBucketLocation",
                    "s3:GetObject",
                    "s3:ListBucket",
                    "s3:PutObject",
                ],
                resources: [
                    `arn:aws:s3:::${athenaOutputBucketParam.valueAsString}`,
                    `arn:aws:s3:::${athenaOutputBucketParam.valueAsString}/athena-output/*`,
                ],
            }),
            new PolicyStatement({
                sid: "S3SourceDataRead",
                effect: Effect.ALLOW,
                actions: [
                    "s3:GetObject",
                    "s3:ListBucket",
                ],
                resources: [
                    `arn:aws:s3:::com-gu-${this.account}-load-balancer-access-logs-eu-west-1`,
                    `arn:aws:s3:::com-gu-${this.account}-load-balancer-access-logs-eu-west-1/*`,
                ],
            }),
            new PolicyStatement({
                sid: "CloudWatchPutMetrics",
                effect: Effect.ALLOW,
                actions: [
                    "cloudwatch:PutMetricData",
                ],
                resources: ["*"],
            }),
        ]

        new GuScheduledLambda(
            this,
            "PlayAccessLogsToMetricsLambda",
            {
                functionName: `${appName}-${props.stage}`,
                app: appName,
                fileName: "play-access-logs-to-metrics-assembly-1.0.0.jar",
                handler: "com.gu.alb.Handler::handleRequest",
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
                runtime: Runtime.JAVA_25,
                environment: {
                    STAGE: props.stage,
                    PLAY_ACCESS_LOGS_CONFIG: playAccessLogsConfigParam.valueAsString,
                    ATHENA_OUTPUT_LOCATION: `s3://${athenaOutputBucketParam.valueAsString}/athena-output/`,
                },
                initialPolicy: lambdaPolicies,
            },
        );
    }
}
