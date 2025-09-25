package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.domain.diamonds.DiamondsCounts;
import com.hpvvssalesautomation.domain.diamonds.DiamondsSummaryResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryResolverTests {

    @Test
    void returnsNoStonesWhenEmpty() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("No Stones");
    }

    @Test
    void returnsProposingWhenAnyProposalExists() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 3, 1, 0, 0, 0, 0, 0, 0, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("Proposing");
    }

    @Test
    void returnsNotApprovedWhenAllAreRejected() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 2, 0, 2, 0, 0, 0, 0, 0, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("Not Approved");
    }

    @Test
    void returnsOnTheWayWhenAnyTransitExists() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 2, 0, 0, 1, 0, 0, 0, 0, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("On the Way");
    }

    @Test
    void returnsKeepWhenAllDeliveredAndChosenToKeep() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 1, 0, 0, 0, 1, 1, 1, 0, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("Keep");
    }

    @Test
    void returnsReturnPendingWhenAnyReturnExists() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 2, 0, 0, 0, 1, 1, 0, 1, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("Return Pending");
    }

    @Test
    void returnsReplacementNeededWhenAnyReplaceExists() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 2, 0, 0, 0, 0, 0, 0, 0, 1);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("Replacement Needed");
    }

    @Test
    void returnsInProgressOtherwise() {
        DiamondsCounts counts = new DiamondsCounts("ROOT", 1, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(DiamondsSummaryResolver.resolve(counts)).isEqualTo("In Progress");
    }
}
