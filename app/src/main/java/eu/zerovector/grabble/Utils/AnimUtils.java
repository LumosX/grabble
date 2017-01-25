package eu.zerovector.grabble.Utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

// 'Twas quite difficult to come up with a name for this class, I do have to admit...
public final class AnimUtils {
    private static final int DEFAULT_GENERIC_CLICK_ANIM_DURATION = 100;
    private static final int DEFAULT_GENERIC_ASH_ANIM_DURATION = 1250;
    private static final int DEFAULT_BUTTON_ANIM_START_COLOUR = 0x53ffffff; // equivalent to UI_WhiteTranslucent

    // Decided to write all the overloads for some reason
    // EDIT: there used to be another parameter and four more overloads, so it was a bit more bothersome than it looks
    public static void DoGenericOnClickAnim(Button targetButton) {
        DoGenericOnClickAnim(targetButton, DEFAULT_GENERIC_CLICK_ANIM_DURATION);
    }

    public static void DoGenericOnClickAnim(Button targetButton, final int colourFrom, final int colourTo) {
        DoGenericOnClickAnim(targetButton, colourFrom, colourTo, DEFAULT_GENERIC_CLICK_ANIM_DURATION);
    }

    public static void DoGenericOnClickAnim(final Button targetButton, int durationMsec) {
        int to = ((ColorDrawable)targetButton.getBackground()).getColor(); // on most (all) buttons this should be UI_GreyTranslucent
        DoGenericOnClickAnim(targetButton, DEFAULT_BUTTON_ANIM_START_COLOUR, to, durationMsec);
    }

    public static void DoGenericOnClickAnim(final Button targetButton, final int colourFrom, final int colourTo,
                                            int durationMsec) {
        final ArgbEvaluator colourEval = new ArgbEvaluator();
        ValueAnimator animator = new ValueAnimator();
        animator.setInterpolator(new LinearInterpolator());
        animator.setFloatValues(0.0f, 1.0f);
        animator.setDuration(durationMsec);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float ratio = valueAnimator.getAnimatedFraction();
                int curColour = (int)colourEval.evaluate(ratio, colourFrom, colourTo);
                targetButton.setBackgroundColor(curColour);;
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                targetButton.setBackgroundColor(colourTo);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                targetButton.setBackgroundColor(colourTo);
            }
        });
        animator.start();
    }


    // ContextCompat doesn't include the 'HTML.fromHTML' method, and I use it for animations, so here it is
    // I know, it's a vague link, but I had to put something else in this sodding file.
    // Also, capital method names are very C#-y, I know. But they're STATIC and need to stand out. That's my excuse.
    public static Spanned FromHTML(String htmlString) {
        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(htmlString);
        }
        return result;
    }

    // And whilst I'm here, might's'well pull this out into a method - I do use it on at least four screens
    // From now on, it also animates the ash icon - a quick flash of white, then fade to normal
    public static void DoGenericAshAnim(final TextView ashAmountLabel, final ImageView ashIcon,
                                        int targetAshAmount) {
        DoGenericAshAnim(ashAmountLabel, ashIcon, targetAshAmount, DEFAULT_GENERIC_ASH_ANIM_DURATION);
    }

    public static void DoGenericAshAnim(final TextView ashAmountLabel, final ImageView ashIcon,
                                        final int targetAshAmount, int durationMsec) {
        int oldAsh = Integer.valueOf(ashAmountLabel.getText().toString());

        // Truncate super-long values, i.e. ones starting from 10 million.
        int maxNonTruncatedAsh = 9999999;
        final boolean truncate = (targetAshAmount > 9999999);
        final String truncatedString = maxNonTruncatedAsh + "+";

        final int startingColour = 0xffffffff;
        final int endingColour = 0x0000000;
        final ArgbEvaluator colourEval = new ArgbEvaluator();
        if (oldAsh != targetAshAmount) {
            ValueAnimator animator = new ValueAnimator();
            animator.setIntValues(oldAsh, targetAshAmount);
            animator.setFloatValues(0.0f, 1.0f); // This actually seems to work, which is pretty good
            animator.setDuration(durationMsec);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    ashAmountLabel.setText((truncate) ? truncatedString :
                            String.valueOf((int)animation.getAnimatedValue()));
                    int curColour = (int)colourEval.evaluate(animation.getAnimatedFraction(), startingColour, endingColour);
                    ashIcon.setColorFilter(curColour);
                }
            });
            // Apparently, Ash values don't always get animated to the same value...
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ashAmountLabel.setText((truncate) ? truncatedString : String.valueOf(targetAshAmount));
                    ashIcon.setColorFilter(endingColour);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    ashAmountLabel.setText((truncate) ? truncatedString : String.valueOf(targetAshAmount));
                    ashIcon.setColorFilter(endingColour);
                }
            });
            animator.start();
        }
    }
}
