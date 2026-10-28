package org.jellyfin.androidtv.ui.browsing;

import android.app.AlertDialog;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.GridDirection;
import org.jellyfin.androidtv.constant.ImageType;
import org.jellyfin.androidtv.constant.PosterSize;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.HorizontalGridBrowseBinding;
import org.jellyfin.androidtv.databinding.PopupEmptyBinding;
import org.jellyfin.androidtv.preference.LibraryPreferences;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.koin.java.KoinJavaComponent;
import org.jellyfin.androidtv.preference.constant.CardShape;
import org.jellyfin.androidtv.data.model.LibraryMediaType;
import org.jellyfin.androidtv.preference.PreferencesRepository;
import org.jellyfin.androidtv.ui.AlphaPickerView;
import org.jellyfin.androidtv.ui.home.SideMenuFrame;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapterHelperKt;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.HorizontalGridPresenter;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyResponse;
import org.jellyfin.sdk.api.client.ApiClient;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.CollectionType;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.SortOrder;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import kotlin.Lazy;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class BrowseGridFragment extends Fragment implements View.OnKeyListener {
    private final static int CHUNK_SIZE_MINIMUM = 25;

    private String mainTitle;
    private FragmentActivity mActivity;
    private BaseRowItem mCurrentItem;
    private CompositeClickedListener mClickedListener = new CompositeClickedListener();
    private CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    private final Handler mHandler = new Handler();
    private int mCardHeight;
    private BrowseRowDef mRowDef;
    private CardPresenter mCardPresenter;

    private boolean justLoaded = true;
    private PosterSize mPosterSizeSetting = PosterSize.MED;
    private ImageType mImageType = ImageType.POSTER;
    private CardShape mCardShape = CardShape.LANDSCAPE;
    private boolean mFavoritesShortcut = false;
    private GridDirection mGridDirection = GridDirection.HORIZONTAL;
    private BrowseGridLayout mLayout;
    private boolean mCompactList = false;
    private boolean determiningPosterSize = false;

    private UUID mParentId;
    private BaseItemDto mFolder;
    private LibraryPreferences libraryPreferences;

    private HorizontalGridBrowseBinding binding;
    private ItemRowAdapter mAdapter;
    private Presenter mGridPresenter;
    private Presenter.ViewHolder mGridViewHolder;
    private BaseGridView mGridView;
    private int mSelectedPosition = -1;
    private int mGridHeight = -1;
    private int mGridWidth = -1;
    private int mGridItemSpacingHorizontal = 0;
    private int mGridItemSpacingVertical = 0;
    private int mGridPaddingLeft = 0;
    private int mGridPaddingTop = 0;

    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private final Lazy<PreferencesRepository> preferencesRepository = inject(PreferencesRepository.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    private final Lazy<ItemLauncher> itemLauncher = inject(ItemLauncher.class);
    private final Lazy<KeyProcessor> keyProcessor = inject(KeyProcessor.class);
    private final Lazy<ApiClient> api = inject(ApiClient.class);

    private int mCardsScreenEst = 0;
    private int mCardsScreenStride = 0;
    private double mCardFocusScale = 1.15; // 115%, just a default we use the resource card_scale_focus otherwise
    private final int MIN_NUM_CARDS = 5; // minimum number of visible cards we allow, this results in more empty space
    private final double CARD_SPACING_PCT = 1.0; // 100% expressed as relative to the padding_left/top, which depends on the mCardFocusScale and AspectRatio
    private final double CARD_SPACING_HORIZONTAL_BANNER_PCT = 0.5; // 50% allow horizontal card overlapping for banners, otherwise spacing is too large
    private final int VIEW_SELECT_UPDATE_DELAY = 250; // delay in ms until we update the top-row info for a selected item

    private boolean mDirty = true; // CardHeight, RowDef or GridSize changed

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init with some working defaults
        DisplayMetrics display = requireContext().getResources().getDisplayMetrics();
        // top + bottom in dp, elements scale with density so adjust accordingly
        mGridHeight = Math.round(display.heightPixels / getResources().getDisplayMetrics().density - 130.6f);
        mGridWidth = Math.round(display.widthPixels / getResources().getDisplayMetrics().density);
        if (BrowseGridLayout.Companion.isEnabled()) {
            // The side menu takes the left edge and the taller preview pushes the grid down
            mGridWidth -= BrowseGridLayout.SIDE_MENU_WIDTH_DP + BrowseGridLayout.GRID_START_MARGIN_DP + BrowseGridLayout.RIGHT_COLUMNS_WIDTH_DP;
            mGridHeight -= BrowseGridLayout.EXTRA_PREVIEW_HEIGHT_DP;
            // Inside a tabbed library page the tab row takes the top
            if (getArguments().getBoolean(Extras.Embedded, false)) mGridHeight -= LibraryTabs.TAB_BAR_HEIGHT_DP;
        }


        mActivity = getActivity();

        mFolder = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), getArguments().getString(Extras.Folder));
        mParentId = mFolder.getId();
        mainTitle = getArguments().getString(Extras.FilterTitle, mFolder.getName());
        libraryPreferences = preferencesRepository.getValue().getLibraryPreferences(Objects.requireNonNull(mFolder.getDisplayPreferencesId()));
        mPosterSizeSetting = libraryPreferences.get(LibraryPreferences.Companion.getPosterSize());
        mImageType = libraryPreferences.get(LibraryPreferences.Companion.getImageType());
        mCardShape = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getLibraryCardShape());
        mGridDirection = libraryPreferences.get(LibraryPreferences.Companion.getGridDirection());
        mCompactList = CompactLibraryKt.supportsCompactList(mFolder.getCollectionType()) && libraryPreferences.get(LibraryPreferences.Companion.getCompactList());
        mCardFocusScale = getResources().getFraction(R.fraction.card_scale_focus, 1, 1);

        if (mCompactList || mGridDirection.equals(GridDirection.VERTICAL))
            setGridPresenter(new VerticalGridPresenter(mCompactList ? FocusHighlight.ZOOM_FACTOR_NONE : FocusHighlight.ZOOM_FACTOR_LARGE, false));
        else
            setGridPresenter(new HorizontalGridPresenter());

        sortOptions = new HashMap<>();
        {
            boolean isTvShows = mFolder.getCollectionType() == CollectionType.TVSHOWS;

            sortOptions.put(0, new SortOption(getString(R.string.lbl_name), ItemSortBy.SORT_NAME, SortOrder.ASCENDING));
            if (isTvShows) {
                sortOptions.put(1, new SortOption(getString(R.string.lbl_date_show_added), ItemSortBy.DATE_CREATED, SortOrder.DESCENDING));
            } else {
                sortOptions.put(1, new SortOption(getString(R.string.lbl_date_added), ItemSortBy.DATE_CREATED, SortOrder.DESCENDING));
            }
            sortOptions.put(2, new SortOption(getString(R.string.lbl_premier_date), ItemSortBy.PREMIERE_DATE, SortOrder.DESCENDING));
            sortOptions.put(3, new SortOption(getString(R.string.lbl_rating), ItemSortBy.OFFICIAL_RATING, SortOrder.ASCENDING));
            sortOptions.put(4, new SortOption(getString(R.string.lbl_community_rating), ItemSortBy.COMMUNITY_RATING, SortOrder.DESCENDING));
            sortOptions.put(5, new SortOption(getString(R.string.lbl_critic_rating), ItemSortBy.CRITIC_RATING, SortOrder.DESCENDING));

            if (isTvShows) {
                sortOptions.put(6, new SortOption(getString(R.string.lbl_last_played), ItemSortBy.SERIES_DATE_PLAYED, SortOrder.DESCENDING));
                sortOptions.put(7, new SortOption(getString(R.string.lbl_date_episode_added), ItemSortBy.DATE_LAST_CONTENT_ADDED, SortOrder.DESCENDING));
            } else {
                sortOptions.put(6, new SortOption(getString(R.string.lbl_last_played), ItemSortBy.DATE_PLAYED, SortOrder.DESCENDING));
                if (mFolder.getCollectionType() == CollectionType.MOVIES) {
                    sortOptions.put(7, new SortOption(getString(R.string.lbl_runtime), ItemSortBy.RUNTIME, SortOrder.ASCENDING));
                }
            }
        }

        setDefaultGridRowCols(mPosterSizeSetting, mImageType);
        setAutoCardGridValues();
        setupQueries();
        setupEventListeners();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = HorizontalGridBrowseBinding.inflate(inflater, container, false);
        mLayout = new BrowseGridLayout(binding, () -> mGridView);
        mLayout.setEmbedded(getArguments().getBoolean(Extras.Embedded, false));
        mLayout.setLibrary(SideMenuFrame.libraryOf(mFolder));
        return mLayout.wrap(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        createGrid();
        loadGrid();
        addTools();
        mLayout.installSideColumns(requireContext(), letter -> {
            jumpToLetter(letter);
            return kotlin.Unit.INSTANCE;
        });
        BulkWatchDialogKt.installBulkWatchHost(view, getViewLifecycleOwner(), () -> mAdapter, () -> {
            if (binding != null && mAdapter != null) {
                binding.toolBar.requestFocus();
                mCurrentItem = null;
                mSelectedPosition = 0;
                mGridView.setSelectedPosition(0);
                mAdapter.Retrieve();
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;
        return keyProcessor.getValue().handleKey(keyCode, mCurrentItem, mActivity);
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) mAdapter.getPageRequest().cancel();
        super.onDestroyView();

        binding = null;
        mGridView = null;
    }

    /** Moves focus into the grid (the tabbed library page sends Down from its tabs here). */
    public boolean focusGrid() {
        return mGridView != null && mGridView.getChildCount() > 0 && mGridView.requestFocus();
    }

    private void createGrid() {
        mGridViewHolder = mGridPresenter.onCreateViewHolder(binding.rowsFragment);
        if (mGridViewHolder instanceof HorizontalGridPresenter.ViewHolder) {
            mGridView = ((HorizontalGridPresenter.ViewHolder) mGridViewHolder).getGridView();
            mGridView.setGravity(Gravity.CENTER_VERTICAL);
            ViewGroup.MarginLayoutParams titleMargin = (ViewGroup.MarginLayoutParams) binding.title.getLayoutParams();
            ViewGroup.MarginLayoutParams clockMargin = (ViewGroup.MarginLayoutParams) binding.clock.getLayoutParams();
            mGridView.setPadding(titleMargin.getMarginStart(), mGridPaddingTop, clockMargin.getMarginEnd(), mGridPaddingTop); // prevent initial card cutoffs
        } else if (mGridViewHolder instanceof VerticalGridPresenter.ViewHolder) {
            mGridView = ((VerticalGridPresenter.ViewHolder) mGridViewHolder).getGridView();
            mGridView.setGravity(mLayout.getEnabled() ? Gravity.START : Gravity.CENTER_HORIZONTAL);
            // LumaFin layout: Leanback sizes the grid to its content and centers it, so a short list (one
            // genre or studio) floated in the middle. Fill the width and start at the left instead.
            if (mLayout.getEnabled() && mGridView.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams gridParams = (android.widget.FrameLayout.LayoutParams) mGridView.getLayoutParams();
                gridParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                gridParams.gravity = Gravity.START | Gravity.TOP;
                mGridView.setLayoutParams(gridParams);
            }
            mGridView.setPadding(mGridPaddingLeft, mGridPaddingTop, mGridPaddingLeft, mGridPaddingTop); // prevent initial card cutoffs
            if (mLayout.getEnabled()) {
                // Let the focused card's zoom and outline draw past the grid's edges instead of being cut off
                mGridView.setClipToPadding(false);
                mGridView.setClipChildren(false);
                // The grid's padding leaves room for the zoom, so cards stay inside it and never
                // scroll under the status line below
                binding.rowsFragment.setClipChildren(true);
            }
        }
        mGridView.setHorizontalSpacing(mGridItemSpacingHorizontal);
        mGridView.setVerticalSpacing(mGridItemSpacingVertical);
        mGridView.setFocusable(true);
        binding.rowsFragment.removeAllViews();
        binding.rowsFragment.addView(mGridViewHolder.view);

        updateAdapter();
    }

    private void updateAdapter() {
        if (mGridView != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
            if (mSelectedPosition != -1) {
                mGridView.setSelectedPosition(mSelectedPosition);
            }
        }
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(HorizontalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        gridPresenter.setOnItemViewClickedListener(mClickedListener);
        gridPresenter.setShadowEnabled(false);
        mGridPresenter = gridPresenter;
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        gridPresenter.setOnItemViewClickedListener(mClickedListener);
        gridPresenter.setShadowEnabled(false);
        mGridPresenter = gridPresenter;
    }


    public void setItem(BaseRowItem item) {
        if (item != null) {
            binding.title.setText(item.getFullName(requireContext()));
            InfoLayoutHelper.addInfoRow(requireContext(), item.getBaseItem(), binding.infoRow, true);
            mLayout.setItem(item.getBaseItem());
        } else {
            binding.title.setText("");
            binding.infoRow.removeAllViews();
            mLayout.setItem(null);
        }
    }

    public class SortOption {
        public String name;
        public ItemSortBy value;
        public SortOrder order;

        public SortOption(String name, ItemSortBy value, SortOrder order) {
            this.name = name;
            this.value = value;
            this.order = order;
        }
    }

    private Map<Integer, SortOption> sortOptions;

    private SortOption getSortOption(ItemSortBy value) {
        for (SortOption sortOption : sortOptions.values()) {
            if (sortOption.value.equals(value)) return sortOption;
        }

        return new SortOption(getString(R.string.lbl_bracket_unknown), ItemSortBy.SORT_NAME, SortOrder.ASCENDING);
    }

    public void setStatusText(String folderName) {
        String text = getString(R.string.lbl_showing) + " ";
        FilterOptions filters = mAdapter.getFilters();
        if (filters == null || (!filters.isFavoriteOnly() && !filters.isUnwatchedOnly())) {
            text += getString(R.string.lbl_all_items);
        } else {
            text += (filters.isUnwatchedOnly() ? getString(R.string.lbl_unwatched) : "") + " " +
                    (filters.isFavoriteOnly() ? getString(R.string.lbl_favorites) : "");
        }

        if (mAdapter.getStartLetter() != null) {
            text += " " + getString(R.string.lbl_starting_with) + " " + mAdapter.getStartLetter();
        }

        text += " " + getString(R.string.lbl_from) + " '" + folderName + "' " + getString(R.string.lbl_sorted_by) + " " + getSortOption(mAdapter.getSortBy()).name + " / " + getString(mAdapter.getSortOrder() == SortOrder.ASCENDING
                ? R.string.library_sort_ascending : R.string.library_sort_descending);

        if (mAdapter.getJumpLetter() != null) text += " · " + getString(R.string.grid_from_letter, mAdapter.getJumpLetter());
        if (filters != null && filters.getGenre() != null) text +=" \u00b7 " + filters.getGenre();
        if (filters != null && filters.getYear() != null) text += " \u00b7 " + filters.getYear();
        if (filters != null && filters.getMinimumRating() != null) text += " \u00b7 " + getString(R.string.library_rating_minimum, filters.getMinimumRating());
        if (filters != null && filters.getResolution() != org.jellyfin.androidtv.data.model.LibraryResolution.ANY) {
            text += " \u00b7 " + getString(filters.getResolution().getLabelRes());
        }
        if (filters != null && filters.getRuntimeMinutes() != null) {
            text += " \u00b7 " + getString(R.string.runtime_check_under, filters.getRuntimeMinutes());
        }
        binding.statusText.setText(text);
    }

    final private OnItemViewSelectedListener mRowSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridView.getSelectedPosition();
                    Timber.d("row selected position %s", position);
                    if (position != mSelectedPosition) {
                        mSelectedPosition = position;
                    }
                    // Update the counter
                    updateCounter(position + 1);
                    if (position >= 0) {
                        mSelectedListener.onItemSelected(itemViewHolder, item, rowViewHolder, row);
                    }
                }
            };

    public void updateCounter(int position) {
        if (mAdapter != null) {
            binding.counter.setText(MessageFormat.format("{0} | {1}", position, mAdapter.getTotalItems()));
        }
    }

    private void setRowDef(final BrowseRowDef rowDef) {
        if (mRowDef == null || mRowDef.hashCode() != rowDef.hashCode()) {
            mDirty = true;
        }
        mRowDef = rowDef;
    }

    /** Portrait cards (#123) apply where the library shows Poster images; Thumb and Banner keep their shapes. */
    private boolean isPortrait() {
        return mCardShape == CardShape.PORTRAIT && mImageType == ImageType.POSTER;
    }

    private double getCardWidthBy(final double cardHeight, ImageType imageType, BaseItemDto folder) {
        switch (imageType) {
            case POSTER:
                // special handling for square posters
                BaseItemKind fType = folder.getType();
                if (fType == BaseItemKind.AUDIO || fType == BaseItemKind.GENRE || fType == BaseItemKind.MUSIC_ALBUM || fType == BaseItemKind.MUSIC_ARTIST || fType == BaseItemKind.MUSIC_GENRE) {
                    return cardHeight;
                } else if (fType == BaseItemKind.COLLECTION_FOLDER && CollectionType.MUSIC.equals(folder.getCollectionType())) {
                    return cardHeight;
                } else {
                    return cardHeight * (isPortrait() ? ImageHelper.ASPECT_RATIO_2_3 : ImageHelper.ASPECT_RATIO_16_9);
                }
            case THUMB:
                return cardHeight * ImageHelper.ASPECT_RATIO_16_9;
            case BANNER:
                return cardHeight * ImageHelper.ASPECT_RATIO_BANNER;
            default:
                throw new IllegalStateException("Unexpected value: " + imageType);
        }
    }

    private double getCardHeightBy(final double cardWidth, ImageType imageType, BaseItemDto folder) {
        switch (imageType) {
            case POSTER:
                // special handling for square posters
                BaseItemKind fType = folder.getType();
                if (fType == BaseItemKind.AUDIO || fType == BaseItemKind.GENRE || fType == BaseItemKind.MUSIC_ALBUM || fType == BaseItemKind.MUSIC_ARTIST || fType == BaseItemKind.MUSIC_GENRE) {
                    return cardWidth;
                } else if (fType == BaseItemKind.COLLECTION_FOLDER && CollectionType.MUSIC.equals(folder.getCollectionType())) {
                    return cardWidth;
                } else {
                    return cardWidth / (isPortrait() ? ImageHelper.ASPECT_RATIO_2_3 : ImageHelper.ASPECT_RATIO_16_9);
                }
            case THUMB:
                return cardWidth / ImageHelper.ASPECT_RATIO_16_9;
            case BANNER:
                return cardWidth / ImageHelper.ASPECT_RATIO_BANNER;
            default:
                throw new IllegalArgumentException("Unexpected value: " + imageType);
        }
    }

    private void setDefaultGridRowCols(PosterSize posterSize, ImageType imageType) {
        if (mCompactList) {
            ((VerticalGridPresenter) mGridPresenter).setNumberOfColumns(1);
            return;
        }
        // HINT: use uneven Rows/Cols if possible, so selected middle lines up with TV middle!
        if (mGridPresenter instanceof VerticalGridPresenter) {
            int numCols;
            switch (posterSize) {
                // Sized to match the cards of the side-scrolling grid at the same poster size
                case SMALLEST:
                    numCols = imageType.equals(ImageType.BANNER) ? 3 : imageType.equals(ImageType.THUMB) ? 5 : 6;
                    break;
                case SMALL:
                    numCols = imageType.equals(ImageType.BANNER) ? 3 : imageType.equals(ImageType.THUMB) ? 4 : 5;
                    break;
                case MED:
                    numCols = imageType.equals(ImageType.BANNER) ? 2 : imageType.equals(ImageType.THUMB) ? 3 : 4;
                    break;
                case LARGE:
                    numCols = imageType.equals(ImageType.BANNER) ? 2 : imageType.equals(ImageType.THUMB) ? 3 : 3;
                    break;
                case X_LARGE:
                    numCols = imageType.equals(ImageType.BANNER) ? 1 : imageType.equals(ImageType.THUMB) ? 2 : 2;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mPosterSizeSetting);
            }
            // Portrait posters are narrower, so more fit across (#123)
            if (isPortrait()) {
                switch (posterSize) {
                    case SMALLEST: numCols = 9; break;
                    case SMALL: numCols = 8; break;
                    case MED: numCols = 7; break;
                    case LARGE: numCols = 5; break;
                    default: numCols = 4; break;
                }
            }
            ((VerticalGridPresenter) mGridPresenter).setNumberOfColumns(numCols);
        } else if (mGridPresenter instanceof HorizontalGridPresenter) {
            int numRows;
            switch (posterSize) {
                case SMALLEST:
                    numRows = imageType.equals(ImageType.BANNER) ? 13 : imageType.equals(ImageType.THUMB) ? 7 : 5;
                    break;
                case SMALL:
                    numRows = imageType.equals(ImageType.BANNER) ? 11 : imageType.equals(ImageType.THUMB) ? 6 : 4;
                    break;
                case MED:
                    numRows = imageType.equals(ImageType.BANNER) ? 9 : imageType.equals(ImageType.THUMB) ? 5 : 3;
                    break;
                case LARGE:
                    numRows = imageType.equals(ImageType.BANNER) ? 7 : imageType.equals(ImageType.THUMB) ? 4 : 2;
                    break;
                case X_LARGE:
                    numRows = imageType.equals(ImageType.BANNER) ? 5 : imageType.equals(ImageType.THUMB) ? 2 : 1;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mPosterSizeSetting);
            }
            // Portrait posters are taller, so fewer rows fit (#123)
            if (isPortrait()) {
                switch (posterSize) {
                    case SMALLEST: numRows = 3; break;
                    case SMALL:
                    case MED: numRows = 2; break;
                    default: numRows = 1; break;
                }
            }
            ((HorizontalGridPresenter) mGridPresenter).setNumberOfRows(numRows);
        }
    }

    private void setAutoCardGridValues() {
        if (mGridPresenter == null) {
            Timber.e("Invalid presenter, cant calculate CardGridValues!");
            return;
        }
        if (mCompactList) {
            mCardHeight = 96;
            mGridItemSpacingHorizontal = 0;
            mGridItemSpacingVertical = Utils.convertDpToPixel(mActivity, 8);
            mGridPaddingLeft = Utils.convertDpToPixel(mActivity, 32);
            mGridPaddingTop = Utils.convertDpToPixel(mActivity, 8);
            mCardsScreenEst = Math.max(1, (mGridHeight + 103) / 104);
            mCardsScreenStride = 1;
            return;
        }
        double cardScaling = Math.max(mCardFocusScale - 1.0, 0.0);
        int cardHeightInt = 100;
        int spacingHorizontalInt = 0;
        int spacingVerticalInt = 0;
        int paddingLeftInt = 0;
        int paddingTopInt = 0;
        int numRows = 0;
        int numCols = 0;
        int numCardsScreen = 0; // number of cards visible, including cutoff's

        if (mGridPresenter instanceof HorizontalGridPresenter) {
            numRows = ((HorizontalGridPresenter) mGridPresenter).getNumberOfRows();
            if (numRows == 1) { // reduce size so minimal cards are shown
                numRows = 0;
                numCols = MIN_NUM_CARDS;
            }
        } else if (mGridPresenter instanceof VerticalGridPresenter) {
            numCols = ((VerticalGridPresenter) mGridPresenter).getNumberOfColumns();
        }

        if (numRows > 0) {
            double paddingPct = cardScaling / numRows;
            double spacingPct = ((paddingPct / 2.0) * CARD_SPACING_PCT) * (numRows - 1);

            double wastedSpacePct = paddingPct + spacingPct;
            double usableCardSpace = mGridHeight / (1.0 + wastedSpacePct); // decrease size
            double cardHeight = usableCardSpace / numRows;

            // fix any rounding errors and make pixel perfect
            cardHeightInt = (int) Math.round(cardHeight);
            double cardPaddingTopBottomAdj = cardHeightInt * cardScaling;
            spacingVerticalInt = Math.max((int) (Math.round((cardPaddingTopBottomAdj / 2.0) * CARD_SPACING_PCT)), 0); // round spacing
            int paddingTopBottomInt = mGridHeight - ((cardHeightInt * numRows) + (spacingVerticalInt * (numRows - 1)));
            paddingTopInt = Math.max(paddingTopBottomInt / 2, 0);

            int sumSize = (cardHeightInt * numRows) + (spacingVerticalInt * (numRows - 1)) + (paddingTopInt * 2);
            if (Math.abs(sumSize - mGridHeight) > 2) {
                Timber.w("setAutoCardGridValues calculation delta > 2, something is off GridHeight <%s> sumSize <%s>!", mGridHeight, sumSize);
            }
            int cardWidthInt = (int) getCardWidthBy(cardHeightInt, mImageType, mFolder);
            paddingLeftInt = (int) Math.round((cardWidthInt * cardScaling) / 2.0);
            spacingHorizontalInt = Math.max((int) (Math.round(paddingLeftInt * CARD_SPACING_PCT)), 0); // round spacing
            if (mImageType == ImageType.BANNER) {
                spacingHorizontalInt = Math.max((int) (Math.round(paddingLeftInt * CARD_SPACING_HORIZONTAL_BANNER_PCT)), 0); // round spacing
            }
            int cardsCol = (int) Math.round(((double) mGridWidth / (cardWidthInt + spacingHorizontalInt)) + 0.5);
            mCardsScreenEst = numRows * cardsCol;
            mCardsScreenStride = numRows;
        } else if (numCols > 0) {
            double paddingPct = cardScaling / numCols;
            double spacingPct = ((paddingPct / 2.0) * CARD_SPACING_PCT) * (numCols - 1);
            if (mImageType == ImageType.BANNER) {
                spacingPct = ((paddingPct / 2.0) * CARD_SPACING_HORIZONTAL_BANNER_PCT) * (numCols - 1);
            }

            double wastedSpacePct = paddingPct + spacingPct;
            double usableCardSpace = mGridWidth / (1.0 + wastedSpacePct); // decrease size
            double cardWidth = usableCardSpace / numCols;

            // fix any rounding errors and make pixel perfect
            cardHeightInt = (int) Math.round(getCardHeightBy(cardWidth, mImageType, mFolder));
            int cardWidthInt = (int) getCardWidthBy(cardHeightInt, mImageType, mFolder);
            double cardPaddingLeftRightAdj = cardWidthInt * cardScaling;
            spacingHorizontalInt = Math.max((int) (Math.round((cardPaddingLeftRightAdj / 2.0) * CARD_SPACING_PCT)), 0); // round spacing
            if (mImageType == ImageType.BANNER) {
                spacingHorizontalInt = Math.max((int) (Math.round((cardPaddingLeftRightAdj / 2.0) * CARD_SPACING_HORIZONTAL_BANNER_PCT)), 0); // round spacing
            }
            int paddingLeftRightInt = mGridWidth - ((cardWidthInt * numCols) + (spacingHorizontalInt * (numCols - 1)));
            paddingLeftInt = Math.max(paddingLeftRightInt / 2, 0);

            int sumSize = (cardWidthInt * numCols) + (spacingHorizontalInt * (numCols - 1)) + (paddingLeftInt * 2);
            if (Math.abs(sumSize - mGridWidth) > 2) {
                Timber.w("setAutoCardGridValues calculation delta > 2, something is off GridWidth <%s> sumSize <%s>!", mGridWidth, sumSize);
            }
            paddingTopInt = (int) Math.round((cardHeightInt * cardScaling) / 2.0);
            spacingVerticalInt = Math.max((int) (Math.round(paddingTopInt * CARD_SPACING_PCT)), 0); // round spacing
            int cardsRow = (int) Math.round(((double) mGridHeight / (cardHeightInt + spacingVerticalInt)) + 0.5);
            mCardsScreenEst = numCols * cardsRow;
            mCardsScreenStride = numCols;
        }

        if (numCols > 0 && BrowseGridLayout.Companion.isEnabled()) {
            // LumaFin layout: the same gap between cards as Home rows, and the first column lined up with
            // the row titles and preview text instead of the grid's edge. Padding and spacing are pixels.
            double inset = BrowseGridLayout.GRID_CONTENT_INSET_DP;
            double gap = BrowseGridLayout.GRID_CARD_GAP_DP;
            double cardWidth = (mGridWidth - 2 * inset - gap * (numCols - 1)) / numCols;
            cardHeightInt = (int) Math.round(getCardHeightBy(cardWidth, mImageType, mFolder));
            spacingHorizontalInt = Utils.convertDpToPixel(mActivity, (int) gap);
            spacingVerticalInt = spacingHorizontalInt;
            paddingLeftInt = Utils.convertDpToPixel(mActivity, (int) inset);
            paddingTopInt = Utils.convertDpToPixel(mActivity, (int) Math.round((cardHeightInt * cardScaling) / 2.0) + 4);
            int cardsRow = (int) Math.round(((double) mGridHeight / (cardHeightInt + gap)) + 0.5);
            mCardsScreenEst = numCols * cardsRow;
        }

        Timber.d("numCardsScreen <%s>", numCardsScreen);

        if (mCardHeight != cardHeightInt) {
            mDirty = true;
        }
        mCardHeight = cardHeightInt;
        mGridItemSpacingHorizontal = spacingHorizontalInt;
        mGridItemSpacingVertical = spacingVerticalInt;
        mGridPaddingLeft = paddingLeftInt;
        mGridPaddingTop = paddingTopInt;
    }

    private void setupQueries() {
        if (mFolder.getType() == BaseItemKind.USER_VIEW || mFolder.getType() == BaseItemKind.COLLECTION_FOLDER) {
            CollectionType type = mFolder.getCollectionType() != null ? mFolder.getCollectionType() : CollectionType.UNKNOWN;
            if (type == CollectionType.MUSIC) {
                //Special queries needed for album artists
                String includeType = getArguments().getString(Extras.IncludeType, null);
                if ("AlbumArtist".equals(includeType)) {
                    setRowDef(new BrowseRowDef("", BrowsingUtils.createAlbumArtistsRequest(mParentId), CHUNK_SIZE_MINIMUM, new ChangeTriggerType[]{}));
                    return;
                } else if ("Artist".equals(includeType)) {
                    setRowDef(new BrowseRowDef("", BrowsingUtils.createArtistsRequest(mParentId), CHUNK_SIZE_MINIMUM, new ChangeTriggerType[]{}));
                    return;
                }
            }
        }

        // The tabbed library pages show a library's collections, or one genre or studio, in this grid
        if ("BoxSet".equals(getArguments().getString(Extras.IncludeType, null))) {
            setRowDef(new BrowseRowDef("", BrowsingUtils.createCollectionsRequest(mParentId), CHUNK_SIZE_MINIMUM, false, true));
            return;
        }
        setRowDef(new BrowseRowDef("", LibraryTabs.filtered(BrowsingUtils.createBrowseGridItemsRequest(mFolder),
                getArguments().getString(Extras.Genre, null), getArguments().getString(Extras.StudioId, null)), CHUNK_SIZE_MINIMUM, false, true));
    }

    @Override
    public void onResume() {
        super.onResume();

        PosterSize posterSizeSetting = libraryPreferences.get(LibraryPreferences.Companion.getPosterSize());
        ImageType imageType = libraryPreferences.get(LibraryPreferences.Companion.getImageType());
        CardShape cardShape = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getLibraryCardShape());
        GridDirection gridDirection = libraryPreferences.get(LibraryPreferences.Companion.getGridDirection());

        boolean compactList = CompactLibraryKt.supportsCompactList(mFolder.getCollectionType()) && libraryPreferences.get(LibraryPreferences.Companion.getCompactList());

        if (mCompactList != compactList || mImageType != imageType || mCardShape != cardShape || mPosterSizeSetting != posterSizeSetting || mGridDirection != gridDirection || mDirty) {
            determiningPosterSize = true;
            mDirty = true;
            mCompactList = compactList;

            mImageType = imageType;
            mCardShape = cardShape;
            mPosterSizeSetting = posterSizeSetting;
            mGridDirection = gridDirection;

            if (mCompactList || mGridDirection.equals(GridDirection.VERTICAL)) {
                setGridPresenter(new VerticalGridPresenter(mCompactList ? FocusHighlight.ZOOM_FACTOR_NONE : FocusHighlight.ZOOM_FACTOR_LARGE, false));
            } else {
                setGridPresenter(new HorizontalGridPresenter());
            }
            setDefaultGridRowCols(mPosterSizeSetting, mImageType);
            setAutoCardGridValues();
            createGrid();
            loadGrid();
            determiningPosterSize = false;
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mAdapter != null) {
                mHandler.postDelayed(() -> {
                    if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        return;

                    if (mAdapter != null && mAdapter.size() > 0) {
                        if (!mAdapter.ReRetrieveIfNeeded()) {
                            refreshCurrentItem();
                        }
                    }
                }, 500);
            }
        } else {
            justLoaded = false;
        }
    }

    private void buildAdapter() {
        if (mAdapter != null) mAdapter.getPageRequest().cancel();
        if (mAdapter != null && mAdapter.getRuntimeController() != null) mAdapter.getRuntimeController().clear();
        mCardPresenter = new CardPresenter(false, mImageType, mCardHeight, true, false, mCompactList, 150, isPortrait());

        Timber.d("buildAdapter cardHeight <%s> getCardWidthBy <%s> chunks <%s> type <%s>", mCardHeight, (int) getCardWidthBy(mCardHeight, mImageType, mFolder), mRowDef.getChunkSize(), mRowDef.getQueryType().toString());

        // adapt chunk size if needed
        int chunkSize = BrowsePageSizeKt.browsePageSize(mRowDef.getChunkSize(), mCardsScreenEst, mCardsScreenStride);
        Timber.d("Browse page size <%s> visible <%s> stride <%s>", chunkSize, mCardsScreenEst, mCardsScreenStride);

        switch (mRowDef.getQueryType()) {
            case NextUp:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getNextUpQuery(), true, mCardPresenter, null);
                break;
            case Views:
                mAdapter = new ItemRowAdapter(requireContext(), GetUserViewsRequest.INSTANCE, mCardPresenter, null);
                break;
            case SimilarSeries:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, null);
                break;
            case SimilarMovies:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, null);
                break;
            case LiveTvChannel:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getTvChannelQuery(), 40, mCardPresenter, null);
                break;
            case LiveTvProgram:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getProgramQuery(), mCardPresenter, null);
                break;
            case LiveTvRecording:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getRecordingQuery(), chunkSize, mCardPresenter, null);
                break;
            case Artists:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getArtistsQuery(), chunkSize, mCardPresenter, null);
                break;
            case AlbumArtists:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getAlbumArtistsQuery(), chunkSize, mCardPresenter, null);
                break;
            default:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getQuery(), chunkSize, mRowDef.getPreferParentThumb(), mRowDef.isStaticHeight(), mCardPresenter, null);
                break;
        }
        mDirty = false;
        if (mFolder.getCollectionType() == CollectionType.MOVIES) {
            mAdapter.setRuntimeController(BrowseGridFragmentHelperKt.createRuntimeController(this, mAdapter, () -> {
                if (binding != null) {
                    if (mUnwatchedButton != null) LibraryFilterMenuKt.updateHideWatchedButton(mUnwatchedButton, mAdapter.getFilters().isUnwatchedOnly());
                    if (mFavoriteButton != null) mFavoriteButton.setActivated(mAdapter.getFilters().isFavoriteOnly());
                    updateDisplayPrefs();
                }
                return kotlin.Unit.INSTANCE;
            }));
        }

        FilterOptions filters = new FilterOptions();
        // A pinned Favorites shortcut (#96) opens with only favorites, leaving the library's own setting alone
        mFavoritesShortcut = getArguments() != null && getArguments().getBoolean(Extras.FavoritesOnly, false);
        filters.setFavoriteOnly(mFavoritesShortcut || libraryPreferences.get(LibraryPreferences.Companion.getFilterFavoritesOnly()));
        filters.setUnwatchedOnly(libraryPreferences.get(LibraryPreferences.Companion.getFilterUnwatchedOnly()));
        String savedGenre = libraryPreferences.get(LibraryPreferences.Companion.getFilterGenre());
        filters.setGenre(savedGenre.isEmpty() ? null : savedGenre);
        int savedYear = libraryPreferences.get(LibraryPreferences.Companion.getFilterYear());
        filters.setYear(savedYear > 0 ? savedYear : null);
        int savedRating = libraryPreferences.get(LibraryPreferences.Companion.getFilterMinimumRating());
        filters.setMinimumRating(savedRating > 0 && savedRating <= 10 ? savedRating : null);
        if (LibraryMediaType.appliesTo(mFolder.getCollectionType())) {
            filters.setMediaType(libraryPreferences.get(LibraryPreferences.Companion.getFilterMediaType()));
        }
        if (mFolder.getCollectionType() == CollectionType.MOVIES) {
            filters.setResolution(libraryPreferences.get(LibraryPreferences.Companion.getFilterResolution()));
            int duration = libraryPreferences.get(LibraryPreferences.Companion.getFilterRuntimeMinutes());
            if (duration == 30 || duration == 60 || duration == 90 || duration == 120) filters.setRuntimeMinutes(duration);
        }

        mAdapter.setRetrieveFinishedListener(new EmptyResponse(getLifecycle()) {
            @Override
            public void onError(Exception exception) {
                BrowsePageFailureKt.showBrowsePageFailure(BrowseGridFragment.this, mAdapter, exception);
            }

            @Override
            public void onResponse() {
                if (!isActive()) return;
                setStatusText(mFolder.getName());
                if (mCurrentItem == null) { // don't mess-up pos via loadMoreItemsIfNeeded
                    setItem(null);
                    updateCounter(mAdapter.getTotalItems() > 0 ? 1 : 0);
                }
                boolean sortedByName = ItemSortBy.SORT_NAME.equals(mAdapter.getSortBy());
                // The LumaFin layout's letter strip replaces the filter-by-letter button
                mLetterButton.setVisibility(sortedByName && !mLayout.getEnabled() ? View.VISIBLE : View.GONE);
                mLayout.setLettersVisible(sortedByName);
                mLayout.setJumpLetter(mAdapter.getJumpLetter());
                if (mAdapter.getItemsLoaded() == 0) {
                    mGridView.setFocusable(false);
                    mHandler.postDelayed(() -> {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                            return;

                        binding.title.setText(mFolder.getName());
                    }, 500);
                } else if (mGridView != null) {
                    mGridView.setFocusable(true);
                    // A completed background page must not pull focus away from the toolbar.
                    if (mCurrentItem == null || mAdapter.getItemsLoaded() <= mAdapter.getRuntimePageSize()) mGridView.requestFocus();
                }
            }
        });
        mAdapter.setFilters(filters);

        updateAdapter();
    }

    public void loadGrid() {
        if (mCardPresenter == null || mAdapter == null || mDirty) {
            buildAdapter();
        }

        SortOption savedSort = getSortOption(libraryPreferences.get(LibraryPreferences.Companion.getSortBy()));
        mAdapter.setSortBy(new SortOption(savedSort.name, savedSort.value,
                libraryPreferences.get(LibraryPreferences.Companion.getSortOrder())));
        mAdapter.Retrieve();
    }

    /** Shows the list from the letter onward; "#" returns to the start. */
    private void jumpToLetter(String letter) {
        if (mAdapter == null) return;
        mSelectedPosition = 0;
        mCurrentItem = null;
        mAdapter.setJumpLetter("#".equals(letter) ? null : letter);
        mLayout.setJumpLetter(letter);
        mAdapter.Retrieve();
    }

    private ImageButton mSortButton;
    private ImageButton mSettingsButton;
    private android.widget.TextView mUnwatchedButton;
    private ImageButton mFavoriteButton;
    private ImageButton mLetterButton;

    private void updateDisplayPrefs() {
        FilterOptions active = mAdapter.getFilters();
        libraryPreferences.set(LibraryPreferences.Companion.getFilterGenre(), active.getGenre() == null ? "" : active.getGenre());
        libraryPreferences.set(LibraryPreferences.Companion.getFilterYear(), active.getYear() == null ? 0 : active.getYear());
        libraryPreferences.set(LibraryPreferences.Companion.getFilterMinimumRating(), active.getMinimumRating() == null ? 0 : active.getMinimumRating());
        libraryPreferences.set(LibraryPreferences.Companion.getFilterResolution(), active.getResolution());
        libraryPreferences.set(LibraryPreferences.Companion.getFilterMediaType(), active.getMediaType());
        libraryPreferences.set(LibraryPreferences.Companion.getFilterRuntimeMinutes(), active.getRuntimeMinutes() == null ? 0 : active.getRuntimeMinutes());
        libraryPreferences.saveBrowsingChoices(mFavoritesShortcut ? libraryPreferences.get(LibraryPreferences.Companion.getFilterFavoritesOnly()) : mAdapter.getFilters().isFavoriteOnly(),
                mAdapter.getFilters().isUnwatchedOnly(), mAdapter.getSortBy(), mAdapter.getSortOrder());
        setStatusText(mFolder.getName());
    }

    private void addTools() {
        //Add tools
        int size = Utils.convertDpToPixel(requireContext(), 26);

        mSortButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mSortButton.setImageResource(R.drawable.ic_sort);
        mSortButton.setMaxHeight(size);
        mSortButton.setAdjustViewBounds(true);
        mSortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create sort menu
                PopupMenu sortMenu = new PopupMenu(getActivity(), binding.toolBar, Gravity.END);
                for (Map.Entry<Integer, SortOption> entry : sortOptions.entrySet()) {
                    MenuItem item = sortMenu.getMenu().add(0, entry.getKey(), entry.getKey(), entry.getValue().name);
                    item.setChecked(entry.getValue().value.equals(libraryPreferences.get(LibraryPreferences.Companion.getSortBy())));
                }
                sortMenu.getMenu().setGroupCheckable(0, true, true);
                sortMenu.getMenu().add(1, 100, 100, getString(R.string.library_reverse_sort)).setCheckable(true)
                        .setChecked(mAdapter.getSortOrder() == SortOrder.DESCENDING);
                sortMenu.getMenu().add(1, 101, 101, getString(R.string.library_reset_browsing));
                sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId() == 101) {
                            mAdapter.setFilters(new FilterOptions());
                            mAdapter.setStartLetter(null);
                            mAdapter.setSortBy(getSortOption(ItemSortBy.SORT_NAME));
                            if (mUnwatchedButton != null) LibraryFilterMenuKt.updateHideWatchedButton(mUnwatchedButton, false);
                            mFavoriteButton.setActivated(false);
                        } else if (item.getItemId() == 100) {
                            SortOption current = getSortOption(mAdapter.getSortBy());
                            mAdapter.setSortBy(new SortOption(current.name, current.value,
                                    mAdapter.getSortOrder() == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING));
                        } else {
                            mAdapter.setSortBy(Objects.requireNonNull(sortOptions.get(item.getItemId())));
                        }
                        mAdapter.Retrieve();
                        item.setChecked(true);
                        updateDisplayPrefs();
                        return true;
                    }
                });
                sortMenu.show();
            }
        });
        mSortButton.setContentDescription(getString(R.string.lbl_sort_by));

        binding.toolBar.addView(mSortButton);

        if (mRowDef.getQueryType() == QueryType.Items) {
            mUnwatchedButton = LibraryFilterMenuKt.createHideWatchedButton(requireContext());
            LibraryFilterMenuKt.updateHideWatchedButton(mUnwatchedButton, mAdapter.getFilters().isUnwatchedOnly());
            mUnwatchedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FilterOptions filters = mAdapter.getFilters();
                    if (filters == null) filters = new FilterOptions();

                    filters.setUnwatchedOnly(!filters.isUnwatchedOnly());
                    LibraryFilterMenuKt.updateHideWatchedButton(mUnwatchedButton, filters.isUnwatchedOnly());
                    mAdapter.setFilters(filters);
                    mAdapter.Retrieve();
                    updateDisplayPrefs();
                }
            });
            binding.toolBar.addView(mUnwatchedButton);
            ImageButton combinedFilters = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
            combinedFilters.setImageResource(R.drawable.ic_filter);
            combinedFilters.setMaxHeight(size);
            combinedFilters.setAdjustViewBounds(true);
            combinedFilters.setContentDescription(getString(R.string.library_filters_title));
            combinedFilters.setOnClickListener(v -> BrowseGridFragmentHelperKt.showCombinedFilters(this, mAdapter, api.getValue(), mFolder.getCollectionType() == CollectionType.MOVIES, LibraryMediaType.appliesTo(mFolder.getCollectionType()), () -> {
                LibraryFilterMenuKt.updateHideWatchedButton(mUnwatchedButton, mAdapter.getFilters().isUnwatchedOnly());
                mFavoriteButton.setActivated(mAdapter.getFilters().isFavoriteOnly());
                updateDisplayPrefs();
                mAdapter.Retrieve();
                return kotlin.Unit.INSTANCE;
            }));
            binding.toolBar.addView(combinedFilters);
        }

        mFavoriteButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mFavoriteButton.setImageResource(R.drawable.ic_heart);
        mFavoriteButton.setActivated(mAdapter.getFilters().isFavoriteOnly());
        mFavoriteButton.setMaxHeight(size);
        mFavoriteButton.setAdjustViewBounds(true);
        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterOptions filters = mAdapter.getFilters();
                if (filters == null) filters = new FilterOptions();

                filters.setFavoriteOnly(!filters.isFavoriteOnly());
                mFavoriteButton.setActivated(filters.isFavoriteOnly());
                mAdapter.setFilters(filters);
                mAdapter.Retrieve();
                updateDisplayPrefs();
            }
        });
        mFavoriteButton.setContentDescription(getString(R.string.lbl_favorite));
        binding.toolBar.addView(mFavoriteButton);

        JumplistPopup jumplistPopup = new JumplistPopup();
        mLetterButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mLetterButton.setImageResource(R.drawable.ic_jump_letter);
        mLetterButton.setMaxHeight(size);
        mLetterButton.setAdjustViewBounds(true);
        mLetterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open letter jump popup
                jumplistPopup.show();
            }
        });
        mLetterButton.setContentDescription(getString(R.string.lbl_by_letter));
        binding.toolBar.addView(mLetterButton);

        if (mFolder.getDisplayPreferencesId() != null) {
            MutableStateFlow<Boolean> settingsVisible = BrowseGridFragmentHelperKt.createSettingsVisibility(BrowseGridFragment.this);
            mSettingsButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
            mSettingsButton.setImageResource(R.drawable.ic_settings);
            mSettingsButton.setMaxHeight(size);
            mSettingsButton.setAdjustViewBounds(true);
            mSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settingsVisible.setValue(true);
                }
            });
            mSettingsButton.setContentDescription(getString(R.string.lbl_settings));
            binding.toolBar.addView(mSettingsButton);
            BrowseGridFragmentHelperKt.addSettings(BrowseGridFragment.this, binding.settings, mFolder.getId(), mFolder.getDisplayPreferencesId(), settingsVisible);
        }
    }

    class JumplistPopup {
        private final int WIDTH = Math.min(Utils.convertDpToPixel(requireContext(), 900),
                getResources().getDisplayMetrics().widthPixels - Utils.convertDpToPixel(requireContext(), 54));
        private final int HEIGHT = Utils.convertDpToPixel(requireContext(), 55);

        private final PopupWindow popupWindow;
        private final AlphaPickerView alphaPicker;
        private kotlinx.coroutines.Job letterLoad;

        JumplistPopup() {
            PopupEmptyBinding layout = PopupEmptyBinding.inflate(getLayoutInflater(), binding.rowsFragment, false);
            layout.emptyPopup.setPadding(0, layout.emptyPopup.getPaddingTop(), 0, layout.emptyPopup.getPaddingBottom());
            popupWindow = new PopupWindow(layout.emptyPopup, WIDTH, HEIGHT, true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setAnimationStyle(R.style.WindowAnimation_SlideTop);

            alphaPicker = new AlphaPickerView(requireContext(), null);
            alphaPicker.setOnAlphaSelected(letter -> {
                dismiss();
                if (letter == '…') {
                    showOtherLetter();
                } else {
                    selectLetter(letter == '*' ? null : letter.toString());
                }
                return null;
            });

            layout.emptyPopup.addView(alphaPicker);
        }

        public void show() {
            alphaPicker.showLoading();
            popupWindow.setWidth(alphaPicker.contentWidth(WIDTH));
            popupWindow.showAtLocation(binding.rowsFragment, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, binding.rowsFragment.getTop());
            alphaPicker.focus('*');
            popupWindow.setOnDismissListener(() -> {
                if (letterLoad != null) letterLoad.cancel(null);
            });
            letterLoad = BrowseGridFragmentHelperKt.loadAvailableLetters(BrowseGridFragment.this, mAdapter,
                    "#" + getString(R.string.byletter_letters), available -> {
                        if (!popupWindow.isShowing()) return null;
                        alphaPicker.setAvailableLetters(available == null ? "" : available);
                        popupWindow.update(alphaPicker.contentWidth(WIDTH), HEIGHT);
                        if (available == null) Utils.showToast(requireContext(), R.string.alpha_load_failed);
                        String selected = mAdapter.getStartLetter();
                        alphaPicker.focus(selected == null || selected.isEmpty() ? '*' : selected.charAt(0));
                        return null;
                    });
        }

        private void selectLetter(String letter) {
            mSelectedPosition = 0;
            mAdapter.setStartLetter(letter);
            loadGrid();
        }

        private void showOtherLetter() {
            android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setHint(R.string.alpha_letter_hint);
            input.setSingleLine(true);
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.alpha_enter_letter)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                String letter = org.jellyfin.androidtv.util.AlphabetInput.normalize(input.getText().toString());
                if (letter == null) {
                    input.setError(getString(R.string.alpha_invalid_letter));
                } else {
                    selectLetter(letter);
                    dialog.dismiss();
                }
            }));
            dialog.show();
        }

        public void dismiss() {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        }
    }

    private void setupEventListeners() {
        if (mGridPresenter != null) {
            if (mGridPresenter instanceof HorizontalGridPresenter)
                ((HorizontalGridPresenter) mGridPresenter).setOnItemViewClickedListener(mClickedListener);
            else if (mGridPresenter instanceof VerticalGridPresenter)
                ((VerticalGridPresenter) mGridPresenter).setOnItemViewClickedListener(mClickedListener);
        }
        mClickedListener.registerListener(new ItemViewClickedListener());
        mSelectedListener.registerListener(new ItemViewSelectedListener());

        CoroutineUtils.readCustomMessagesOnLifecycle(getLifecycle(), customMessageRepository.getValue(), message -> {
            if (message.equals(CustomMessage.RefreshCurrentItem.INSTANCE)) refreshCurrentItem();
            return null;
        });
    }

    private void refreshCurrentItem() {
        if (mCurrentItem == null) return;
        Timber.i("Refresh item \"%s\"", mCurrentItem.getFullName(requireContext()));
        ItemRowAdapterHelperKt.refreshItem(mAdapter, api.getValue(), this, mCurrentItem, () -> {
            //Now - if filtered make sure we still pass
            if (mAdapter.getFilters() == null) return null;
            if ((mAdapter.getFilters().isFavoriteOnly() && !mCurrentItem.isFavorite()) || (mAdapter.getFilters().isUnwatchedOnly() && mCurrentItem.isPlayed())) {
                // if we are about to remove the current item, throw focus to toolbar so framework doesn't crash
                binding.toolBar.requestFocus();
                mAdapter.remove(mCurrentItem);
                mAdapter.setTotalItems(mAdapter.getTotalItems() - 1);
                updateCounter(mAdapter.indexOf(mCurrentItem));
            }
            return null;
        });
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            itemLauncher.getValue().launch((BaseRowItem) item, mAdapter, requireContext());
        }
    }

    private final Runnable mDelayedSetItem = new Runnable() {
        @Override
        public void run() {
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

            backgroundService.getValue().setBackground(mCurrentItem.getBaseItem());
            setItem(mCurrentItem);
        }
    };

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            mHandler.removeCallbacks(mDelayedSetItem);
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                binding.title.setText(mainTitle);
                //fill in default background
                backgroundService.getValue().clearBackgrounds();
            } else {
                mCurrentItem = (BaseRowItem) item;
                binding.title.setText(mCurrentItem.getName(requireContext()));
                binding.infoRow.removeAllViews();
                mHandler.postDelayed(mDelayedSetItem, VIEW_SELECT_UPDATE_DELAY);

                if (!determiningPosterSize)
                    mAdapter.loadMoreItemsIfNeeded(mAdapter.indexOf(mCurrentItem));
            }
        }
    }
}
