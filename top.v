//`include "MainGEMMsplit.v"
module top(
input        top_clk,
input        top_resetn,

//CTRL_BUS -- nnote this is axil
input           axi_CTRL_BUS_ACLK,
input           axi_CTRL_BUS_ARESET,
//input           axi_CTRL_BUS_ACLK_EN,
input  [6:0]    axi_CTRL_BUS_AWADDR,
input           axi_CTRL_BUS_AWVALID,
output          axi_CTRL_BUS_AWREADY,
input  [31:0]   axi_CTRL_BUS_WDATA,
input  [3:0]    axi_CTRL_BUS_WSTRB,
input           axi_CTRL_BUS_WVALID,
output          axi_CTRL_BUS_WREADY,
output [1:0]    axi_CTRL_BUS_BRESP,
output          axi_CTRL_BUS_BVALID,
input           axi_CTRL_BUS_BREADY,
input  [6:0]    axi_CTRL_BUS_ARADDR,
input           axi_CTRL_BUS_ARVALID,
output          axi_CTRL_BUS_ARREADY,
output [31:0]   axi_CTRL_BUS_RDATA,
output [1:0]    axi_CTRL_BUS_RRESP,
output          axi_CTRL_BUS_RVALID,
input           axi_CTRL_BUS_RREADY,
output          interrupt,

//BUS_A
output          axi_BUS_A_aruser,
output          axi_BUS_A_awuser,
output [5:0]    axi_BUS_A_awid,
output [48:0]   axi_BUS_A_awaddr,
output [7:0]    axi_BUS_A_awlen,
output [2:0]    axi_BUS_A_awsize,
output [1:0]    axi_BUS_A_awburst,
output          axi_BUS_A_awlock,
output [3:0]    axi_BUS_A_awcache,
output [2:0]    axi_BUS_A_awprot,
output          axi_BUS_A_awvalid,
input           axi_BUS_A_awready,
output [127:0]  axi_BUS_A_wdata,
output [15:0]   axi_BUS_A_wstrb,
output          axi_BUS_A_wlast,
output          axi_BUS_A_wvalid,
input           axi_BUS_A_wready,
input [5:0]     axi_BUS_A_bid,
input [1:0]     axi_BUS_A_bresp,
input           axi_BUS_A_bvalid,
output          axi_BUS_A_bready,
output [5:0]    axi_BUS_A_arid,
output [48:0]   axi_BUS_A_araddr,
output [7:0]    axi_BUS_A_arlen,
output [2:0]    axi_BUS_A_arsize,
output [1:0]    axi_BUS_A_arburst,
output          axi_BUS_A_arlock,
output [3:0]    axi_BUS_A_arcache,
output [2:0]    axi_BUS_A_arprot,
output          axi_BUS_A_arvalid,
input           axi_BUS_A_arready,
input [5:0]     axi_BUS_A_rid,
input [127:0]   axi_BUS_A_rdata,
input [1:0]     axi_BUS_A_rresp,
input           axi_BUS_A_rlast,
input           axi_BUS_A_rvalid,
output          axi_BUS_A_rready,
output [3:0]    axi_BUS_A_awqos,
output [3:0]    axi_BUS_A_arqos,

//BUS_B
output          axi_BUS_B_aruser,
output          axi_BUS_B_awuser,
output [5:0]    axi_BUS_B_awid,
output [48:0]   axi_BUS_B_awaddr,
output [7:0]    axi_BUS_B_awlen,
output [2:0]    axi_BUS_B_awsize,
output [1:0]    axi_BUS_B_awburst,
output          axi_BUS_B_awlock,
output [3:0]    axi_BUS_B_awcache,
output [2:0]    axi_BUS_B_awprot,
output          axi_BUS_B_awvalid,
input           axi_BUS_B_awready,
output [127:0]  axi_BUS_B_wdata,
output [15:0]   axi_BUS_B_wstrb,
output          axi_BUS_B_wlast,
output          axi_BUS_B_wvalid,
input           axi_BUS_B_wready,
input [5:0]     axi_BUS_B_bid,
input [1:0]     axi_BUS_B_bresp,
input           axi_BUS_B_bvalid,
output          axi_BUS_B_bready,
output [5:0]    axi_BUS_B_arid,
output [48:0]   axi_BUS_B_araddr,
output [7:0]    axi_BUS_B_arlen,
output [2:0]    axi_BUS_B_arsize,
output [1:0]    axi_BUS_B_arburst,
output          axi_BUS_B_arlock,
output [3:0]    axi_BUS_B_arcache,
output [2:0]    axi_BUS_B_arprot,
output          axi_BUS_B_arvalid,
input           axi_BUS_B_arready,
input [5:0]     axi_BUS_B_rid,
input [127:0]   axi_BUS_B_rdata,
input [1:0]     axi_BUS_B_rresp,
input           axi_BUS_B_rlast,
input           axi_BUS_B_rvalid,
output          axi_BUS_B_rready,
output [3:0]    axi_BUS_B_awqos,
output [3:0]    axi_BUS_B_arqos,

//BUS_C
output          axi_BUS_C_aruser,
output          axi_BUS_C_awuser,
output [5:0]    axi_BUS_C_awid,
output [48:0]   axi_BUS_C_awaddr,
output [7:0]    axi_BUS_C_awlen,
output [2:0]    axi_BUS_C_awsize,
output [1:0]    axi_BUS_C_awburst,
output          axi_BUS_C_awlock,
output [3:0]    axi_BUS_C_awcache,
output [2:0]    axi_BUS_C_awprot,
output          axi_BUS_C_awvalid,
input           axi_BUS_C_awready,
output [127:0]  axi_BUS_C_wdata,
output [15:0]   axi_BUS_C_wstrb,
output          axi_BUS_C_wlast,
output          axi_BUS_C_wvalid,
input           axi_BUS_C_wready,
input [5:0]     axi_BUS_C_bid,
input [1:0]     axi_BUS_C_bresp,
input           axi_BUS_C_bvalid,
output          axi_BUS_C_bready,
output [5:0]    axi_BUS_C_arid,
output [48:0]   axi_BUS_C_araddr,
output [7:0]    axi_BUS_C_arlen,
output [2:0]    axi_BUS_C_arsize,
output [1:0]    axi_BUS_C_arburst,
output          axi_BUS_C_arlock,
output [3:0]    axi_BUS_C_arcache,
output [2:0]    axi_BUS_C_arprot,
output          axi_BUS_C_arvalid,
input           axi_BUS_C_arready,
input [5:0]     axi_BUS_C_rid,
input [127:0]   axi_BUS_C_rdata,
input [1:0]     axi_BUS_C_rresp,
input           axi_BUS_C_rlast,
input           axi_BUS_C_rvalid,
output          axi_BUS_C_rready,
output [3:0]    axi_BUS_C_awqos,
output [3:0]    axi_BUS_C_arqos,


input        axi_BUS_A_aclk,
input        axi_BUS_B_aclk,
input        axi_BUS_C_aclk
);

//A wires
wire [48:0]    a_addr_wire;
wire         a_addr_ready_wire;
wire        a_addr_valid_wire;
wire [127:0]     a_data_wire;
wire         a_data_ready_wire;
wire        a_data_valid_wire;
//B wires
wire [48:0]     b_addr_wire;
wire         b_addr_ready_wire;
wire        b_addr_valid_wire;
wire [127:0]     b_data_wire;
wire         b_data_ready_wire;
wire        b_data_valid_wire;
//C wires
wire [48:0]     c_addr_wire;
wire         c_addr_ready_wire;
wire        c_addr_valid_wire;
wire [127:0]     c_data_wire;
wire         c_data_ready_wire;
wire        c_data_valid_wire;

wire        bit_zero;
wire [48:0]    addr_zero;
wire [127:0]    data_zero;

assign bit_zero = 1'b0;
assign addr_zero = 49'b0;
assign data_zero = 128'b0;

// stupid wires
wire        ap_start;
wire        ap_done;
wire        ap_ready;
wire        ap_idle;
wire [63:0] A;
wire [63:0] B;
wire [63:0] C;
wire [31:0] m;
wire [31:0] n;
wire [31:0] k;

assign ap_idle = 1'b1;
assign ap_ready = 1'b1; // TODO fix

stupid s(
    .ACLK(axi_CTRL_BUS_ACLK),
    .ARESET(axi_CTRL_BUS_ARESET),
    .ACLK_EN(1'b1), // hardwired to true
    .AWADDR(axi_CTRL_BUS_AWADDR),
    .AWVALID(axi_CTRL_BUS_AWVALID),
    .AWREADY(axi_CTRL_BUS_AWREADY),
    .WDATA(axi_CTRL_BUS_WDATA),
    .WSTRB(axi_CTRL_BUS_WSTRB),
    .WVALID(axi_CTRL_BUS_WVALID),
    .WREADY(axi_CTRL_BUS_WREADY),
    .BRESP(axi_CTRL_BUS_BRESP),
    .BVALID(axi_CTRL_BUS_BVALID),
    .BREADY(axi_CTRL_BUS_BREADY),
    .ARADDR(axi_CTRL_BUS_ARADDR),
    .ARVALID(axi_CTRL_BUS_ARVALID),
    .ARREADY(axi_CTRL_BUS_ARREADY),
    .RDATA(axi_CTRL_BUS_RDATA),
    .RRESP(axi_CTRL_BUS_RRESP),
    .RVALID(axi_CTRL_BUS_RVALID),
    .RREADY(axi_CTRL_BUS_RREADY),
    .interrupt(interrupt),
    .ap_start(ap_start),
    .ap_done(ap_done),
    .ap_ready(ap_ready),
    .ap_idle(ap_idle),
    .A(A),
    .B(B),
    .C(C),
    .m(m),
    .n(n),
    .k(k)
);

MainGEMM gemm(
    .clock            (top_clk        ),
    .reset            (~top_resetn        ),

    .io_ctrl_start (ap_start),
    .io_ctrl_finished (ap_done),
    .io_ctrl_cmd_a_addr (A),
    .io_ctrl_cmd_b_addr (B),
    .io_ctrl_cmd_c_addr (C),
    .io_ctrl_cmd_m (m),
    .io_ctrl_cmd_n (n),
    .io_ctrl_cmd_k (k),

    .io_mem_a_addr_ready    (a_addr_ready_wire    ),
    .io_mem_a_addr_valid    (a_addr_valid_wire    ),
    .io_mem_a_addr_bits     (a_addr_wire        ),
    .io_mem_a_data_ready    (a_data_ready_wire    ),
    .io_mem_a_data_valid    (a_data_valid_wire    ),
    .io_mem_a_data_bits     (a_data_wire        ),
    .io_mem_b_addr_ready    (b_addr_ready_wire    ),
    .io_mem_b_addr_valid    (b_addr_valid_wire    ),
    .io_mem_b_addr_bits     (b_addr_wire        ),
    .io_mem_b_data_ready    (b_data_ready_wire    ),
    .io_mem_b_data_valid    (b_data_valid_wire    ),
    .io_mem_b_data_bits     (b_data_wire        ),
    .io_mem_c_addr_ready    (c_addr_ready_wire    ),
    .io_mem_c_addr_valid    (c_addr_valid_wire    ),
    .io_mem_c_addr_bits     (c_addr_wire        ),
    .io_mem_c_data_ready    (c_data_ready_wire    ),
    .io_mem_c_data_valid    (c_data_valid_wire    ),
    .io_mem_c_data_bits     (c_data_wire        )
);

ACC_AXI aport (
    .acc_araddr    (a_addr_wire        ),
    .acc_arvalid    (a_addr_valid_wire    ),
    .acc_arready    (a_addr_ready_wire    ),
    .acc_rdata    (a_data_wire        ),
    .acc_rvalid    (a_data_valid_wire    ),
    .acc_rready    (a_data_ready_wire    ),    
    .acc_awaddr    (addr_zero        ),
    .acc_awvalid    (bit_zero        ),
    .acc_awready    (    ),
    .acc_wdata    (data_zero        ),
    .acc_wvalid    (bit_zero        ),
    .acc_wready    (        ),
    .axi_aruser    (axi_BUS_A_aruser    ),
    .axi_awuser    (axi_BUS_A_awuser    ),
    .axi_awid    (axi_BUS_A_awid        ),
    .axi_awaddr    (axi_BUS_A_awaddr    ),
    .axi_awlen    (axi_BUS_A_awlen    ),
    .axi_awsize    (axi_BUS_A_awsize    ),
    .axi_awburst    (axi_BUS_A_awburst    ),
    .axi_awlock    (axi_BUS_A_awlock    ),
    .axi_awcache    (axi_BUS_A_awcache    ),
    .axi_awprot    (axi_BUS_A_awprot    ),
    .axi_awvalid    (axi_BUS_A_awvalid    ),
    .axi_awready    (axi_BUS_A_awready    ),
    .axi_wdata    (axi_BUS_A_wdata    ),
    .axi_wstrb    (axi_BUS_A_wstrb    ),
    .axi_wlast    (axi_BUS_A_wlast    ),
    .axi_wvalid    (axi_BUS_A_wvalid    ),
    .axi_wready    (axi_BUS_A_wready    ),
    .axi_bid    (axi_BUS_A_bid        ),
    .axi_bresp    (axi_BUS_A_bresp    ),
    .axi_bvalid    (axi_BUS_A_bvalid    ),
    .axi_bready    (axi_BUS_A_bready    ),
    .axi_arid    (axi_BUS_A_arid        ),
    .axi_araddr    (axi_BUS_A_araddr    ),
    .axi_arlen    (axi_BUS_A_arlen    ),
    .axi_arsize    (axi_BUS_A_arsize    ),
    .axi_arburst    (axi_BUS_A_arburst    ),
    .axi_arlock    (axi_BUS_A_arlock    ),
    .axi_arcache    (axi_BUS_A_arcache    ),
    .axi_arprot    (axi_BUS_A_arprot    ),
    .axi_arvalid    (axi_BUS_A_arvalid    ),
    .axi_arready    (axi_BUS_A_arready    ),
    .axi_rid    (axi_BUS_A_rid        ),
    .axi_rdata    (axi_BUS_A_rdata    ),
    .axi_rresp    (axi_BUS_A_rresp    ),
    .axi_rlast    (axi_BUS_A_rlast    ),
    .axi_rvalid    (axi_BUS_A_rvalid    ),
    .axi_rready    (axi_BUS_A_rready    ),
    .axi_awqos    (axi_BUS_A_awqos    ),
    .axi_arqos    (axi_BUS_A_arqos    )
);

ACC_AXI bport (
    .acc_araddr    (b_addr_wire        ),
    .acc_arvalid    (b_addr_valid_wire    ),
    .acc_arready    (b_addr_ready_wire    ),
    .acc_rdata    (b_data_wire        ),
    .acc_rvalid    (b_data_valid_wire    ),
    .acc_rready    (b_data_ready_wire    ),
    .acc_awaddr    (addr_zero        ),
    .acc_awvalid    (bit_zero        ),
    .acc_awready    (        ),
    .acc_wdata    (data_zero        ),
    .acc_wvalid    (bit_zero        ),
    .acc_wready    (        ),
    .axi_aruser    (axi_BUS_B_aruser    ),
    .axi_awuser    (axi_BUS_B_awuser    ),
    .axi_awid    (axi_BUS_B_awid        ),
    .axi_awaddr    (axi_BUS_B_awaddr    ),
    .axi_awlen    (axi_BUS_B_awlen    ),
    .axi_awsize    (axi_BUS_B_awsize    ),
    .axi_awburst    (axi_BUS_B_awburst    ),
    .axi_awlock    (axi_BUS_B_awlock    ),
    .axi_awcache    (axi_BUS_B_awcache    ),
    .axi_awprot    (axi_BUS_B_awprot    ),
    .axi_awvalid    (axi_BUS_B_awvalid    ),
    .axi_awready    (axi_BUS_B_awready    ),
    .axi_wdata    (axi_BUS_B_wdata    ),
    .axi_wstrb    (axi_BUS_B_wstrb    ),
    .axi_wlast    (axi_BUS_B_wlast    ),
    .axi_wvalid    (axi_BUS_B_wvalid    ),
    .axi_wready    (axi_BUS_B_wready    ),
    .axi_bid    (axi_BUS_B_bid        ),
    .axi_bresp    (axi_BUS_B_bresp    ),
    .axi_bvalid    (axi_BUS_B_bvalid    ),
    .axi_bready    (axi_BUS_B_bready    ),
    .axi_arid    (axi_BUS_B_arid        ),
    .axi_araddr    (axi_BUS_B_araddr    ),
    .axi_arlen    (axi_BUS_B_arlen    ),
    .axi_arsize    (axi_BUS_B_arsize    ),
    .axi_arburst    (axi_BUS_B_arburst    ),
    .axi_arlock    (axi_BUS_B_arlock    ),
    .axi_arcache    (axi_BUS_B_arcache    ),
    .axi_arprot    (axi_BUS_B_arprot    ),
    .axi_arvalid    (axi_BUS_B_arvalid    ),
    .axi_arready    (axi_BUS_B_arready    ),
    .axi_rid    (axi_BUS_B_rid        ),
    .axi_rdata    (axi_BUS_B_rdata    ),
    .axi_rresp    (axi_BUS_B_rresp    ),
    .axi_rlast    (axi_BUS_B_rlast    ),
    .axi_rvalid    (axi_BUS_B_rvalid    ),
    .axi_rready    (axi_BUS_B_rready    ),
    .axi_awqos    (axi_BUS_B_awqos    ),
    .axi_arqos    (axi_BUS_B_arqos    )
);

ACC_AXI cport (
    .acc_araddr    (addr_zero              ),
    .acc_arvalid    (bit_zero        ),
    .acc_arready    (    ),
    .acc_rdata    (    ),
    .acc_rvalid    (    ),
    .acc_rready    (bit_zero        ),    
    .acc_awaddr    (c_addr_wire        ),
    .acc_awvalid    (c_addr_valid_wire    ),
    .acc_awready    (c_addr_ready_wire    ),
    .acc_wdata    (c_data_wire        ),
    .acc_wvalid    (c_data_valid_wire    ),
    .acc_wready    (c_data_ready_wire    ),
    .axi_aruser    (axi_BUS_C_aruser    ),
    .axi_awuser    (axi_BUS_C_awuser    ),
    .axi_awid    (axi_BUS_C_awid        ),
    .axi_awaddr    (axi_BUS_C_awaddr    ),
    .axi_awlen    (axi_BUS_C_awlen    ),
    .axi_awsize    (axi_BUS_C_awsize    ),
    .axi_awburst    (axi_BUS_C_awburst    ),
    .axi_awlock    (axi_BUS_C_awlock    ),
    .axi_awcache    (axi_BUS_C_awcache    ),
    .axi_awprot    (axi_BUS_C_awprot    ),
    .axi_awvalid    (axi_BUS_C_awvalid    ),
    .axi_awready    (axi_BUS_C_awready    ),
    .axi_wdata    (axi_BUS_C_wdata    ),
    .axi_wstrb    (axi_BUS_C_wstrb    ),
    .axi_wlast    (axi_BUS_C_wlast    ),
    .axi_wvalid    (axi_BUS_C_wvalid    ),
    .axi_wready    (axi_BUS_C_wready    ),
    .axi_bid    (axi_BUS_C_bid        ),
    .axi_bresp    (axi_BUS_C_bresp    ),
    .axi_bvalid    (axi_BUS_C_bvalid    ),
    .axi_bready    (axi_BUS_C_bready    ),
    .axi_arid    (axi_BUS_C_arid        ),
    .axi_araddr    (axi_BUS_C_araddr    ),
    .axi_arlen    (axi_BUS_C_arlen    ),
    .axi_arsize    (axi_BUS_C_arsize    ),
    .axi_arburst    (axi_BUS_C_arburst    ),
    .axi_arlock    (axi_BUS_C_arlock    ),
    .axi_arcache    (axi_BUS_C_arcache    ),
    .axi_arprot    (axi_BUS_C_arprot    ),
    .axi_arvalid    (axi_BUS_C_arvalid    ),
    .axi_arready    (axi_BUS_C_arready    ),
    .axi_rid    (axi_BUS_C_rid        ),
    .axi_rdata    (axi_BUS_C_rdata    ),
    .axi_rresp    (axi_BUS_C_rresp    ),
    .axi_rlast    (axi_BUS_C_rlast    ),
    .axi_rvalid    (axi_BUS_C_rvalid    ),
    .axi_rready    (axi_BUS_C_rready    ),
    .axi_awqos    (axi_BUS_C_awqos    ),
    .axi_arqos    (axi_BUS_C_arqos    )
);

endmodule

module ACC_AXI(
// to/from accelerator
input [48:0]    acc_araddr,
input           acc_arvalid,
output          acc_arready,
output [127:0]  acc_rdata,
output          acc_rvalid,
input           acc_rready,
input [48:0]    acc_awaddr,
input           acc_awvalid,
output             acc_awready,
input [127:0]      acc_wdata,
input              acc_wvalid,
output          acc_wready,
//To ZYNQ PS
output          axi_aruser,
output          axi_awuser,
output [5:0]    axi_awid,
output [48:0]   axi_awaddr,
output [7:0]    axi_awlen, //0
output [2:0]    axi_awsize, //100
output [1:0]    axi_awburst, //0
output          axi_awlock,
output [3:0]    axi_awcache,
output [2:0]    axi_awprot,
output          axi_awvalid,
input           axi_awready,
output [127:0]  axi_wdata,
output [15:0]   axi_wstrb, //FFFF
output          axi_wlast,
output          axi_wvalid,
input           axi_wready,
input [5:0]     axi_bid,
input [1:0]     axi_bresp,
input           axi_bvalid,
output          axi_bready,
output [5:0]    axi_arid,
output [48:0]   axi_araddr,
output [7:0]    axi_arlen, //0
output [2:0]    axi_arsize, //100
output [1:0]    axi_arburst, //0
output          axi_arlock,
output [3:0]    axi_arcache,
output [2:0]    axi_arprot,
output          axi_arvalid,
input           axi_arready,
input [5:0]     axi_rid,
input [127:0]   axi_rdata,
input [1:0]     axi_rresp,
input           axi_rlast,
input           axi_rvalid,
output          axi_rready,
output [3:0]    axi_awqos,
output [3:0]    axi_arqos
);

//(* keep="soft" *)
//pass these wires from A, B, C

assign acc_rdata = axi_rdata;
assign acc_rvalid = axi_rvalid;
assign axi_rready = acc_rready;
assign axi_araddr = acc_araddr;
assign axi_arvalid = acc_arvalid;
assign acc_arready = axi_arready;

assign axi_wdata = acc_wdata;
assign axi_wvalid = acc_wvalid;
assign acc_wready = axi_wready;
assign axi_awaddr = acc_awaddr;
assign axi_awvalid = acc_awvalid;
assign acc_awready = axi_awready;

assign axi_aruser = 1'b0;
assign axi_awuser = 1'b0;

//write
assign axi_awid = 5'b00000;
assign axi_awlen = 8'b00000000;
assign axi_awsize = 3'b100; //128b
assign axi_awburst = 2'b00;
assign axi_awlock = 1'b0;
assign axi_awcache = 4'b0011; //non-coherent, normal mem
assign axi_awprot = 3'b000;
assign axi_wstrb = 16'hFFFF; //all bits are valid writes; could be a problem?
assign axi_wlast = 1'b1;

//write response
assign axi_bready = 1'b1;

//read
assign axi_arid = 5'b00000;
assign axi_arlen = 8'b00000000;
assign axi_arsize = 3'b100;
assign axi_arburst = 2'b00;
assign axi_arlock = 1'b0;
assign axi_arcache = 4'b0011;
assign axi_arprot = 3'b000;

assign axi_arqos = 4'b0000;
assign axi_awqos = 4'b0000;

endmodule
