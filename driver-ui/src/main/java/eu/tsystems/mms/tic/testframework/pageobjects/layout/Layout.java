/*
 * (C) Copyright T-Systems Multimedia Solutions GmbH 2018, ..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Peter Lehmann <p.lehmann@t-systems.com>
 *     pele <p.lehmann@t-systems.com>
 */
package eu.tsystems.mms.tic.testframework.pageobjects.layout;

import eu.tsystems.mms.tic.testframework.annotations.Fails;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.asserts.IConfiguredAssert;
import eu.tsystems.mms.tic.testframework.pageobjects.internal.facade.IGuiElement;
import eu.tsystems.mms.tic.testframework.utils.JSUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import java.util.Map;

/**
 * @todo Allow access to distance values (required for new assert interface)
 */
public abstract class Layout implements ILayout {

    private boolean innerBorders = false;

    private Layout(boolean innerBorders) {
        this.innerBorders = innerBorders;
    }

    public static ILayout inner() {
        /*
        Dummy impl.
         */
        ILayout layout = new Layout(true) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert configuredAssert) {
                //dummy
            }

            @Override
            public String toStringText() {
                return null;
            }
        };

        return layout;
    }

    public static ILayout outer() {
        /*
        Dummy impl.
         */
        ILayout layout = new Layout(false) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert configuredAssert) {
                //dummy
            }

            @Override
            public String toStringText() {
                return null;
            }
        };

        return layout;
    }

    @Fails(validFor = "unsupportedBrowser", description = "Unsupported Browser")
    public LayoutBorders getElementLayoutBorders(IGuiElement GuiElementFacade) {
        if (innerBorders) {
            Map<String, Long> borders = JSUtils.getElementInnerBorders(GuiElementFacade);
            if (borders != null) {
                return new LayoutBorders(borders.get("left"), borders.get("right"), borders.get("top"), borders.get("bottom"));
            }
            return new LayoutBorders(0, 0, 0, 0);
        } else {
            Point location = GuiElementFacade.getLocation();
            Dimension size = GuiElementFacade.getSize();
            int left = location.getX();
            int right = left + size.getWidth();
            int top = location.getY();
            int bottom = top + size.getHeight();
            return new LayoutBorders(left, right, top, bottom);
        }
    }

    public ILayout leftOf(final IGuiElement distanceGE) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.left;
                long reference = distLB.left;
                IConfiguredAssert.assertTrue(actual < reference, ">" + actualGE + "< is left of >" + distanceGE + "< (" + actual + "<" + reference + ")");
            }

            @Override
            public String toStringText() {
                return "leftOf " + distanceGE;
            }
        };
    }

    public String toString() {
        return toStringText();
    }

    public ILayout above(final IGuiElement distanceGE) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.top;
                long reference = distLB.top;
                IConfiguredAssert.assertTrue(actual < reference, ">" + actualGE + "< is above >" + distanceGE + "< (" + actual + "<" + reference + ")");
            }

            @Override
            public String toStringText() {
                return "above " + distanceGE;
            }
        };
    }

    @Override
    public ILayout rightOf(final IGuiElement distanceGE) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.right;
                long reference = distLB.right;
                IConfiguredAssert.assertTrue(actual > reference, ">" + actualGE + "< is right of >" + distanceGE + "< (" + actual + ">" + reference + ")");
            }

            @Override
            public String toStringText() {
                return "rightOf " + distanceGE;
            }
        };
    }

    public ILayout below(final IGuiElement distanceGE) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.bottom;
                long reference = distLB.bottom;
                IConfiguredAssert.assertTrue(actual > reference, ">" + actualGE + "< is below >" + distanceGE + "< (" + actual + ">" + reference + ")");
            }

            @Override
            public String toStringText() {
                return "below " + distanceGE;
            }
        };
    }

    @Override
    public ILayout sameTop(final IGuiElement distanceGE, final int delta) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.top;
                long reference = distLB.top;

                long max = reference + delta;
                long min = reference - delta;
                IConfiguredAssert.assertTrue(actual <= max && actual >= min, ">" + actualGE + "< has same top coords as >" + distanceGE + "< (" + actual + "==" + reference + " +-" + delta + ")");
            }

            @Override
            public String toStringText() {
                return "same top coords as " + distanceGE;
            }
        };
    }

    @Override
    public ILayout sameBottom(final IGuiElement distanceGE, final int delta) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.bottom;
                long reference = distLB.bottom;

                long max = reference + delta;
                long min = reference - delta;
                IConfiguredAssert.assertTrue(actual <= max && actual >= min, ">" + actualGE + "< has same bottom coords as >" + distanceGE + "< (" + actual + "==" + reference + " +-" + delta + ")");
            }

            @Override
            public String toStringText() {
                return "same bottom coords as " + distanceGE;
            }
        };
    }

    @Override
    public ILayout sameLeft(final IGuiElement distanceGE, final int delta) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.left;
                long reference = distLB.left;

                long max = reference + delta;
                long min = reference - delta;
                IConfiguredAssert.assertTrue(actual <= max && actual >= min, ">" + actualGE + "< has same left coords as >" + distanceGE + "< (" + actual + "==" + reference + " +-" + delta + ")");
            }

            @Override
            public String toStringText() {
                return "same left coords as " + distanceGE;
            }
        };
    }

    @Override
    public ILayout sameRight(final IGuiElement distanceGE, final int delta) {
        return new Layout(this.innerBorders) {
            @Override
            public void checkOn(IGuiElement actualGE, IConfiguredAssert IConfiguredAssert) {
                LayoutBorders actLB = getElementLayoutBorders(actualGE);
                LayoutBorders distLB = getElementLayoutBorders(distanceGE);
                long actual = actLB.right;
                long reference = distLB.right;

                long max = reference + delta;
                long min = reference - delta;
                IConfiguredAssert.assertTrue(actual <= max && actual >= min, ">" + actualGE + "< has same right coords as >" + distanceGE + "< (" + actual + "==" + reference + " +-" + delta + ")");
            }

            @Override
            public String toStringText() {
                return "same right coords as " + distanceGE;
            }
        };
    }

}
