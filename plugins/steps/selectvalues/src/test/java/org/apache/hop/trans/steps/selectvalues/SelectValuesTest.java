/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.trans.steps.selectvalues;

import org.apache.hop.core.Const;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.RowSet;
import org.apache.hop.core.exception.HopConversionException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.core.row.value.ValueMetaBase;
import org.apache.hop.core.row.value.ValueMetaBigNumber;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.junit.rules.RestoreHopEngineEnvironment;
import org.apache.hop.trans.Trans;
import org.apache.hop.trans.TransMeta;
import org.apache.hop.trans.step.StepDataInterface;
import org.apache.hop.trans.step.StepMeta;
import org.apache.hop.trans.steps.mock.StepMockHelper;
import org.apache.hop.trans.steps.selectvalues.SelectValuesMeta.SelectField;
import org.junit.*;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

/**
 * @author Andrey Khayrutdinov
 */
public class SelectValuesTest {
  @ClassRule public static RestoreHopEngineEnvironment env = new RestoreHopEngineEnvironment();

  private static final String SELECTED_FIELD = "field";

  private final Object[] inputRow = new Object[] { "a string" };

  private SelectValues step;
  private StepMockHelper<SelectValuesMeta, StepDataInterface> helper;

  @BeforeClass
  public static void initHop() throws Exception {
    HopEnvironment.init();
  }

  @Before
  public void setUp() throws Exception {
    helper = StepMockUtil.getStepMockHelper( SelectValuesMeta.class, "SelectValuesTest" );
    when( helper.stepMeta.isDoingErrorHandling() ).thenReturn( true );

    step = new SelectValues( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step = spy( step );
    doReturn( inputRow ).when( step ).getRow();
    doNothing().when( step )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    RowMeta inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaString( SELECTED_FIELD ) );
    step.setInputRowMeta( inputRowMeta );
  }

  @After
  public void cleanUp() {
    helper.cleanUp();
  }

  @Test
  public void testPDI16368() throws Exception {
    // This tests that the fix for PDI-16388 doesn't get re-broken.
    //

    SelectValuesHandler step2 = null;
    Object[] inputRow2 = null;
    RowMeta inputRowMeta = null;
    SelectValuesMeta stepMeta = null;
    SelectValuesData stepData = null;
    ValueMetaInterface vmi = null;
    // First, test current behavior (it's worked this way since 5.x or so)
    //
    step2 = new SelectValuesHandler( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step2 = spy( step2 );
    inputRow2 = new Object[] { new BigDecimal( "589" ) }; // Starting with a BigDecimal (no places)
    doReturn( inputRow2 ).when( step2 ).getRow();
    doNothing().when( step2 )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    step2.setInputRowMeta( inputRowMeta );
    stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_INTEGER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask.

    stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;
    step2.processRow( stepMeta, stepData );

    vmi = step2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_BIG_NUMBER_FORMAT_MASK, vmi.getConversionMask() );

    step2 = new SelectValuesHandler( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step2 = spy( step2 );
    doReturn( inputRow2 ).when( step2 ).getRow();
    doNothing().when( step2 )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    step2.setInputRowMeta( inputRowMeta );
    stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_NUMBER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask for Double.

    stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;
    step2.processRow( stepMeta, stepData );

    vmi = step2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_BIG_NUMBER_FORMAT_MASK, vmi.getConversionMask() );


    step2 = new SelectValuesHandler( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step2 = spy( step2 );
    inputRow2 = new Object[] { new Long( "589" ) }; // Starting with a Long
    doReturn( inputRow2 ).when( step2 ).getRow();
    doNothing().when( step2 )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaInteger( SELECTED_FIELD ) );
    step2.setInputRowMeta( inputRowMeta );
    stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    // no specified conversion type so should have default conversion mask for BigNumber
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_BIGNUMBER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null );

    stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;
    step2.processRow( stepMeta, stepData );

    vmi = step2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_INTEGER_FORMAT_MASK, vmi.getConversionMask() );

    // Now, test that setting the variable results in getting the default conversion mask
    step2 = new SelectValuesHandler( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step2.setVariable( Const.HOP_COMPATIBILITY_SELECT_VALUES_TYPE_CHANGE_USES_TYPE_DEFAULTS, "Y" );
    step2 = spy( step2 );
    inputRow2 = new Object[] { new BigDecimal( "589" ) }; // Starting with a BigDecimal (no places)
    doReturn( inputRow2 ).when( step2 ).getRow();
    doNothing().when( step2 )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    step2.setInputRowMeta( inputRowMeta );
    stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_INTEGER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask.

    stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;
    step2.processRow( stepMeta, stepData );

    vmi = step2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_INTEGER_FORMAT_MASK, vmi.getConversionMask() );

    step2 = new SelectValuesHandler( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step2.setVariable( Const.HOP_COMPATIBILITY_SELECT_VALUES_TYPE_CHANGE_USES_TYPE_DEFAULTS, "Y" );
    step2 = spy( step2 );
    doReturn( inputRow2 ).when( step2 ).getRow();
    doNothing().when( step2 )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    step2.setInputRowMeta( inputRowMeta );
    stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_NUMBER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask for Double.

    stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;
    step2.processRow( stepMeta, stepData );

    vmi = step2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_NUMBER_FORMAT_MASK, vmi.getConversionMask() );


    step2 = new SelectValuesHandler( helper.stepMeta, helper.stepDataInterface, 1, helper.transMeta, helper.trans );
    step2.setVariable( Const.HOP_COMPATIBILITY_SELECT_VALUES_TYPE_CHANGE_USES_TYPE_DEFAULTS, "Y" );
    step2 = spy( step2 );
    inputRow2 = new Object[] { new Long( "589" ) }; // Starting with a Long
    doReturn( inputRow2 ).when( step2 ).getRow();
    doNothing().when( step2 )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaInteger( SELECTED_FIELD ) );
    step2.setInputRowMeta( inputRowMeta );
    stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    // no specified conversion type so should have default conversion mask for BigNumber
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_BIGNUMBER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null );

    stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;
    step2.processRow( stepMeta, stepData );

    vmi = step2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_BIG_NUMBER_FORMAT_MASK, vmi.getConversionMask() );

  }

  @Test
  public void errorRowSetObtainsFieldName() throws Exception {
    SelectValuesMeta stepMeta = new SelectValuesMeta();
    stepMeta.allocate( 1, 0, 1 );
    stepMeta.getSelectFields()[ 0 ] = new SelectField();
    stepMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    stepMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( stepMeta, SELECTED_FIELD, null, ValueMetaInterface.TYPE_INTEGER, -2, -2,
        ValueMetaInterface.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null );

    SelectValuesData stepData = new SelectValuesData();
    stepData.select = true;
    stepData.metadata = true;
    stepData.firstselect = true;
    stepData.firstmetadata = true;

    step.processRow( stepMeta, stepData );

    verify( step )
      .putError( any( RowMetaInterface.class ), any( Object[].class ), anyLong(), anyString(), eq( SELECTED_FIELD ),
        anyString() );


    // additionally ensure conversion error causes HopConversionError
    boolean properException = false;
    try {
      step.metadataValues( step.getInputRowMeta(), inputRow );
    } catch ( HopConversionException e ) {
      properException = true;
    }
    assertTrue( properException );
  }

  public class SelectValuesHandler extends SelectValues {
    private Object[] resultRow;
    private RowMetaInterface rowMeta;
    private RowSet rowset;

    public SelectValuesHandler( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
                                Trans trans ) {
      super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
    }

    @Override
    public void putRow( RowMetaInterface rm, Object[] row ) throws HopStepException {
      resultRow = row;
      rowMeta = rm;
    }

    /**
     * Find input row set.
     *
     * @param sourceStep the source step
     * @return the row set
     * @throws HopStepException the kettle step exception
     */
    @Override
    public RowSet findInputRowSet( String sourceStep ) throws HopStepException {
      return rowset;
    }

  }
}
