package cc.fascinated.monitor.alert.engine;

import cc.fascinated.monitor.alert.engine.AlertRuleEngine.Change;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRuleEngineTest {
    private static Change step(RuleSpec spec, SeriesState state, long ts, double value) {
        return AlertRuleEngine.evaluate(spec, state, ts, value).change();
    }

    @Test
    void instantaneousRuleFiresAndResolvesOnTheThreshold() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 90, null, 0);
        SeriesState state = new SeriesState(0);

        assertThat(step(spec, state, 0, 90)).as("equal is not above").isEqualTo(Change.NONE);
        assertThat(step(spec, state, 1, 91)).isEqualTo(Change.FIRED);
        assertThat(step(spec, state, 2, 99)).isEqualTo(Change.NONE);
        assertThat(state.worstValue()).isEqualTo(99);
        assertThat(step(spec, state, 3, 90)).isEqualTo(Change.RESOLVED);
        assertThat(step(spec, state, 4, 95)).isEqualTo(Change.FIRED);
    }

    @Test
    void lessThanTracksTheLowestValueAsWorst() {
        RuleSpec spec = new RuleSpec(AlertOperator.LESS_THAN, 10, null, 0);
        SeriesState state = new SeriesState(0);

        assertThat(step(spec, state, 0, 8)).isEqualTo(Change.FIRED);
        step(spec, state, 1, 3);
        step(spec, state, 2, 6);
        assertThat(state.worstValue()).isEqualTo(3);
        assertThat(step(spec, state, 3, 10)).isEqualTo(Change.RESOLVED);
    }

    @Test
    void recoveryThresholdAddsHysteresis() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 90, 80.0, 0);
        SeriesState state = new SeriesState(0);

        assertThat(step(spec, state, 0, 95)).isEqualTo(Change.FIRED);
        assertThat(step(spec, state, 1, 85)).as("still above the recovery threshold").isEqualTo(Change.NONE);
        assertThat(step(spec, state, 2, 80)).isEqualTo(Change.RESOLVED);
    }

    @Test
    void windowedRuleIgnoresASingleSpike() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 50, null, 100);
        SeriesState state = new SeriesState(100);

        for (long ts = 0; ts <= 200; ts += 10) {
            double value = ts == 120 ? 100 : 0;
            assertThat(step(spec, state, ts, value)).isEqualTo(Change.NONE);
        }
        assertThat(state.isFiring()).isFalse();
    }

    @Test
    void windowedRuleWaitsUntilTheWindowIsCoveredThenFiresOnTheAverage() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 50, null, 100);
        SeriesState state = new SeriesState(100);

        for (long ts = 0; ts < 90; ts += 10) {
            assertThat(step(spec, state, ts, 95)).as("window not covered yet at %d", ts).isEqualTo(Change.NONE);
        }
        assertThat(step(spec, state, 90, 95)).isEqualTo(Change.FIRED);
    }

    @Test
    void windowedRuleResolvesOnceTheAverageDropsBack() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 50, null, 100);
        SeriesState state = new SeriesState(100);
        long ts = 0;
        for (; ts <= 90; ts += 10) {
            step(spec, state, ts, 95);
        }
        assertThat(state.isFiring()).isTrue();

        Change last = Change.NONE;
        for (; ts <= 300 && last != Change.RESOLVED; ts += 10) {
            last = step(spec, state, ts, 0);
        }
        assertThat(last).isEqualTo(Change.RESOLVED);
        assertThat(ts).as("a few samples of recovery are not enough").isGreaterThan(130);
    }

    @Test
    void seriesAreIndependent() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 90, null, 0);
        SeriesState root = new SeriesState(0);
        SeriesState data = new SeriesState(0);

        assertThat(step(spec, root, 0, 95)).isEqualTo(Change.FIRED);
        assertThat(step(spec, data, 0, 10)).isEqualTo(Change.NONE);
        assertThat(root.isFiring()).isTrue();
        assertThat(data.isFiring()).isFalse();
    }

    @Test
    void restoredFiringStateResolvesWithoutRefiring() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 90, null, 0);
        SeriesState state = new SeriesState(0);
        state.restoreFiring(100, 97, 100, null);

        assertThat(step(spec, state, 200, 95)).isEqualTo(Change.NONE);
        assertThat(step(spec, state, 201, 10)).isEqualTo(Change.RESOLVED);
        assertThat(state.firingSince()).isEqualTo(100);
    }

    @Test
    void silencedSeriesStaysQuietUntilItGetsWorse() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 0, null, 0);
        SeriesState state = new SeriesState(0);

        assertThat(step(spec, state, 0, 5)).isEqualTo(Change.FIRED);
        assertThat(state.silence(5)).isTrue();
        assertThat(step(spec, state, 1, 5)).as("same value stays silent").isEqualTo(Change.NONE);
        assertThat(step(spec, state, 2, 4)).as("a better value stays silent").isEqualTo(Change.NONE);
        assertThat(step(spec, state, 3, 6)).as("worse than when silenced").isEqualTo(Change.ESCALATED);
        assertThat(state.isSilenced()).isFalse();
        assertThat(step(spec, state, 4, 7)).as("no longer silenced, no repeat escalation").isEqualTo(Change.NONE);
    }

    @Test
    void silencedLessThanSeriesEscalatesWhenItDrops() {
        RuleSpec spec = new RuleSpec(AlertOperator.LESS_THAN, 10, null, 0);
        SeriesState state = new SeriesState(0);

        assertThat(step(spec, state, 0, 8)).isEqualTo(Change.FIRED);
        state.silence(8);
        assertThat(step(spec, state, 1, 9)).isEqualTo(Change.NONE);
        assertThat(step(spec, state, 2, 7)).isEqualTo(Change.ESCALATED);
    }

    @Test
    void resolvingWhileSilencedIsFlaggedAndClearsTheSilence() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 90, null, 0);
        SeriesState state = new SeriesState(0);

        step(spec, state, 0, 95);
        state.silence(95);
        AlertRuleEngine.Evaluation resolved = AlertRuleEngine.evaluate(spec, state, 1, 50);
        assertThat(resolved.change()).isEqualTo(Change.RESOLVED);
        assertThat(resolved.wasSilenced()).isTrue();
        assertThat(state.isSilenced()).isFalse();
        assertThat(step(spec, state, 2, 95)).as("fires normally afterwards").isEqualTo(Change.FIRED);
        assertThat(state.isSilenced()).isFalse();
    }

    @Test
    void cannotSilenceASeriesThatIsNotFiring() {
        SeriesState state = new SeriesState(0);
        assertThat(state.silence(1)).isFalse();
        assertThat(state.isSilenced()).isFalse();
    }

    @Test
    void restoredSilencedSeriesKeepsItsBaseline() {
        RuleSpec spec = new RuleSpec(AlertOperator.MORE_THAN, 0, null, 0);
        SeriesState state = new SeriesState(0);

        state.restoreFiring(100, 5, 100, 5.0);
        assertThat(state.isSilenced()).isTrue();
        assertThat(step(spec, state, 101, 5)).isEqualTo(Change.NONE);
        assertThat(step(spec, state, 102, 6)).isEqualTo(Change.ESCALATED);
    }
}
