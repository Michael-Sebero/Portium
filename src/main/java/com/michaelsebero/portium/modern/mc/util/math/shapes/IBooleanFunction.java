package com.michaelsebero.portium.modern.mc.util.math.shapes;

/** 1.13's {@code net.minecraft.util.math.shapes.IBooleanFunction}: how two shapes combine, cell by cell. */
public interface IBooleanFunction {

    @com.michaelsebero.portium.modern.Srg("field_223230_a_")
    IBooleanFunction FALSE = (first, second) -> false;
    @com.michaelsebero.portium.modern.Srg("field_223231_b_")
    IBooleanFunction NOT_OR = (first, second) -> !first && !second;
    @com.michaelsebero.portium.modern.Srg("field_223232_c_")
    IBooleanFunction ONLY_SECOND = (first, second) -> second && !first;
    @com.michaelsebero.portium.modern.Srg("field_223233_d_")
    IBooleanFunction NOT_FIRST = (first, second) -> !first;
    @com.michaelsebero.portium.modern.Srg("field_223234_e_")
    IBooleanFunction ONLY_FIRST = (first, second) -> first && !second;
    @com.michaelsebero.portium.modern.Srg("field_223235_f_")
    IBooleanFunction NOT_SECOND = (first, second) -> !second;
    @com.michaelsebero.portium.modern.Srg("field_223236_g_")
    IBooleanFunction NOT_SAME = (first, second) -> first != second;
    @com.michaelsebero.portium.modern.Srg("field_223237_h_")
    IBooleanFunction NOT_AND = (first, second) -> !first || !second;
    @com.michaelsebero.portium.modern.Srg("field_223238_i_")
    IBooleanFunction AND = (first, second) -> first && second;
    @com.michaelsebero.portium.modern.Srg("field_223239_j_")
    IBooleanFunction SAME = (first, second) -> first == second;
    @com.michaelsebero.portium.modern.Srg("field_223240_k_")
    IBooleanFunction SECOND = (first, second) -> second;
    @com.michaelsebero.portium.modern.Srg("field_223241_l_")
    IBooleanFunction CAUSES = (first, second) -> !first || second;
    @com.michaelsebero.portium.modern.Srg("field_223242_m_")
    IBooleanFunction FIRST = (first, second) -> first;
    @com.michaelsebero.portium.modern.Srg("field_223243_n_")
    IBooleanFunction CAUSED_BY = (first, second) -> first || !second;
    @com.michaelsebero.portium.modern.Srg("field_223244_o_")
    IBooleanFunction OR = (first, second) -> first || second;
    @com.michaelsebero.portium.modern.Srg("field_223245_p_")
    IBooleanFunction TRUE = (first, second) -> true;

    boolean apply(boolean first, boolean second);
}
