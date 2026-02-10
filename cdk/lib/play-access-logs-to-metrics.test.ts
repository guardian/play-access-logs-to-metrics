import {App} from "aws-cdk-lib";
import {Template} from "aws-cdk-lib/assertions";
import {PlayAccessLogsToMetrics} from "./play-access-logs-to-metrics";

describe("The PlayAccessLogsToMetrics stack", () => {
    it("matches the snapshot", () => {
        const app = new App();
        const stack = new PlayAccessLogsToMetrics(app, "PlayAccessLogsToMetrics", {stack: "frontend", stage: "TEST"});
        const template = Template.fromStack(stack);
        expect(template.toJSON()).toMatchSnapshot();
    });
});
