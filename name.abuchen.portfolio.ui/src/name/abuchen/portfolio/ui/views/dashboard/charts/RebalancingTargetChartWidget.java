package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.util.function.Supplier;

import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.TaxonomyConfig;
import name.abuchen.portfolio.ui.views.taxonomy.DonutChartBuilder;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode;

public class RebalancingTargetChartWidget extends CircularChartWidget<TaxonomyModel>
{
    private DonutChartBuilder builder = new DonutChartBuilder()
    {
        @Override
        protected Money getMoney(TaxonomyNode node)
        {
            if (node.getTarget() == null)
                return Money.of(node.getActual().getCurrencyCode(), 0);

            return node.getTarget();
        }
    };

    public RebalancingTargetChartWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfigAfter(ClientFilterConfig.class, new TaxonomyConfig(this));
        addConfigAfter(TaxonomyConfig.class, new IncludeUnassignedCategoryConfig(this, true));
    }

    @Override
    protected void configureTooltip(CircularChartToolTip toolTip)
    {
        builder.configureToolTip(toolTip);
    }

    @Override
    public Supplier<TaxonomyModel> getUpdateTask()
    {
        return () -> {
            Taxonomy taxonomy = get(TaxonomyConfig.class).getTaxonomy();

            if (taxonomy != null)
            {
                TaxonomyModel model = new TaxonomyModel(getDashboardData().getExchangeRateProviderFactory(),
                                getClient(), taxonomy);

                // apply filter if applicable
                Client filteredClient = get(ClientFilterConfig.class).getSelectedFilter().filter(getClient());
                if (filteredClient != getClient())
                    model.updateClientSnapshot(filteredClient);

                model.setExcludeUnassignedCategoryInCharts(
                                !get(IncludeUnassignedCategoryConfig.class).isUnassignedCategoryIncluded());

                // we want the target chart to always work on categories (there
                // are no target values for individual positions)
                model.setExcludeSecuritiesInPieChart(true);

                return model;
            }
            else
            {
                return null;
            }
        };
    }

    @Override
    protected void createCircularSeries(TaxonomyModel model)
    {
        if (model != null)
        {
            builder.createCircularSeries(getChart(), model);
        }
        else
        {
            ICircularSeries<?> circularSeries = (ICircularSeries<?>) getChart().getSeriesSet()
                            .createSeries(SeriesType.DOUGHNUT, Messages.LabelErrorNoTaxonomySelected);
            circularSeries.setSliceColor(getChart().getPlotArea().getBackground());
            circularSeries.setSeries(new String[] { Messages.LabelErrorNoTaxonomySelected }, new double[] { 100 });
            circularSeries.setColor(Messages.LabelErrorNoTaxonomySelected, Colors.LIGHT_GRAY);
        }
    }
}
