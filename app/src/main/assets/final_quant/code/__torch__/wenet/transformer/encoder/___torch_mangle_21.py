class ConformerEncoder(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  _output_size : int
  normalize_before : bool
  static_chunk_size : int
  use_dynamic_chunk : bool
  use_dynamic_left_chunk : bool
  global_cmvn : __torch__.wenet.transformer.cmvn.GlobalCMVN
  embed : __torch__.wenet.transformer.subsampling.___torch_mangle_16.Conv2dSubsampling4
  after_norm : __torch__.torch.nn.modules.normalization.LayerNorm
  encoders : __torch__.torch.nn.modules.container.___torch_mangle_20.ModuleList
  def forward(self: __torch__.wenet.transformer.encoder.___torch_mangle_21.ConformerEncoder,
    xs: Tensor,
    xs_lens: Tensor,
    decoding_chunk_size: int=0,
    num_decoding_left_chunks: int=-1) -> Tuple[Tensor, Tensor]:
    _0 = __torch__.wenet.utils.mask.make_pad_mask
    _1 = __torch__.wenet.utils.mask.add_optional_chunk_mask
    masks = torch.bitwise_not(torch.unsqueeze(_0(xs_lens, ), 1))
    xs0 = (self.global_cmvn).forward(xs, )
    _2 = (self.embed).forward(xs0, masks, 0, )
    xs1, pos_emb, masks0, = _2
    chunk_masks = _1(xs1, masks0, self.use_dynamic_chunk, self.use_dynamic_left_chunk, decoding_chunk_size, self.static_chunk_size, num_decoding_left_chunks, )
    _3 = self.encoders
    _4 = getattr(_3, "0")
    _5 = getattr(_3, "1")
    _6 = getattr(_3, "2")
    _7 = getattr(_3, "3")
    _8 = getattr(_3, "4")
    _9 = getattr(_3, "5")
    _10 = getattr(_3, "6")
    _11 = getattr(_3, "7")
    _12 = getattr(_3, "8")
    _13 = getattr(_3, "9")
    _14 = getattr(_3, "10")
    _15 = getattr(_3, "11")
    _16 = (_4).forward(xs1, chunk_masks, pos_emb, masks0, None, None, )
    xs2, chunk_masks0, _17, = _16
    _18 = (_5).forward(xs2, chunk_masks0, pos_emb, masks0, None, None, )
    xs3, chunk_masks1, _19, = _18
    _20 = (_6).forward(xs3, chunk_masks1, pos_emb, masks0, None, None, )
    xs4, chunk_masks2, _21, = _20
    _22 = (_7).forward(xs4, chunk_masks2, pos_emb, masks0, None, None, )
    xs5, chunk_masks3, _23, = _22
    _24 = (_8).forward(xs5, chunk_masks3, pos_emb, masks0, None, None, )
    xs6, chunk_masks4, _25, = _24
    _26 = (_9).forward(xs6, chunk_masks4, pos_emb, masks0, None, None, )
    xs7, chunk_masks5, _27, = _26
    _28 = (_10).forward(xs7, chunk_masks5, pos_emb, masks0, None, None, )
    xs8, chunk_masks6, _29, = _28
    _30 = (_11).forward(xs8, chunk_masks6, pos_emb, masks0, None, None, )
    xs9, chunk_masks7, _31, = _30
    _32 = (_12).forward(xs9, chunk_masks7, pos_emb, masks0, None, None, )
    xs10, chunk_masks8, _33, = _32
    _34 = (_13).forward(xs10, chunk_masks8, pos_emb, masks0, None, None, )
    xs11, chunk_masks9, _35, = _34
    _36 = (_14).forward(xs11, chunk_masks9, pos_emb, masks0, None, None, )
    xs12, chunk_masks10, _37, = _36
    _38 = (_15).forward(xs12, chunk_masks10, pos_emb, masks0, None, None, )
    xs13, chunk_masks11, _39, = _38
    if self.normalize_before:
      xs14 = (self.after_norm).forward(xs13, )
    else:
      xs14 = xs13
    return (xs14, masks0)
  def forward_chunk(self: __torch__.wenet.transformer.encoder.___torch_mangle_21.ConformerEncoder,
    xs: Tensor,
    offset: int,
    required_cache_size: int,
    subsampling_cache: Optional[Tensor]=None,
    elayers_output_cache: Optional[List[Tensor]]=None,
    conformer_cnn_cache: Optional[List[Tensor]]=None) -> Tuple[Tensor, Tensor, List[Tensor], List[Tensor]]:
    if torch.eq(torch.size(xs, 0), 1):
      pass
    else:
      ops.prim.RaiseException("Exception")
    _40 = torch.size(xs, 1)
    _41 = ops.prim.device(xs)
    tmp_masks = torch.ones([1, _40], dtype=11, layout=None, device=_41, pin_memory=None)
    tmp_masks0 = torch.unsqueeze(tmp_masks, 1)
    xs15 = (self.global_cmvn).forward(xs, )
    _42 = (self.embed).forward(xs15, tmp_masks0, offset, )
    xs16, pos_emb, _43, = _42
    _44 = torch.__isnot__(subsampling_cache, None)
    if _44:
      subsampling_cache0 = unchecked_cast(Tensor, subsampling_cache)
      cache_size0 = torch.size(subsampling_cache0, 1)
      xs18 = torch.cat([subsampling_cache0, xs16], 1)
      cache_size, xs17 = cache_size0, xs18
    else:
      cache_size, xs17 = 0, xs16
    pos_emb0 = (self.embed).position_encoding(torch.sub(offset, cache_size), torch.size(xs17, 1), )
    if torch.lt(required_cache_size, 0):
      next_cache_start = 0
    else:
      if torch.eq(required_cache_size, 0):
        next_cache_start0 = torch.size(xs17, 1)
      else:
        _45 = torch.sub(torch.size(xs17, 1), required_cache_size)
        next_cache_start0 = ops.prim.max(_45, 0)
      next_cache_start = next_cache_start0
    _46 = torch.slice(xs17, 0, 0, 9223372036854775807, 1)
    _47 = torch.slice(_46, 1, next_cache_start, 9223372036854775807, 1)
    r_subsampling_cache = torch.slice(_47, 2, 0, 9223372036854775807, 1)
    _48 = torch.size(xs17, 1)
    _49 = ops.prim.device(xs17)
    masks = torch.ones([1, _48], dtype=11, layout=None, device=_49, pin_memory=None)
    masks1 = torch.unsqueeze(masks, 1)
    r_elayers_output_cache = annotate(List[Tensor], [])
    r_conformer_cnn_cache = annotate(List[Tensor], [])
    _50 = self.encoders
    _51 = getattr(_50, "0")
    _52 = getattr(_50, "1")
    _53 = getattr(_50, "2")
    _54 = getattr(_50, "3")
    _55 = getattr(_50, "4")
    _56 = getattr(_50, "5")
    _57 = getattr(_50, "6")
    _58 = getattr(_50, "7")
    _59 = getattr(_50, "8")
    _60 = getattr(_50, "9")
    _61 = getattr(_50, "10")
    _62 = getattr(_50, "11")
    _63 = torch.__is__(elayers_output_cache, None)
    if _63:
      attn_cache, elayers_output_cache0 = None, elayers_output_cache
    else:
      elayers_output_cache1 = unchecked_cast(List[Tensor], elayers_output_cache)
      attn_cache, elayers_output_cache0 = elayers_output_cache1[0], elayers_output_cache1
    _64 = torch.__is__(conformer_cnn_cache, None)
    if _64:
      cnn_cache, conformer_cnn_cache0 = None, conformer_cnn_cache
    else:
      conformer_cnn_cache1 = unchecked_cast(List[Tensor], conformer_cnn_cache)
      cnn_cache, conformer_cnn_cache0 = conformer_cnn_cache1[0], conformer_cnn_cache1
    _65 = (_51).forward(xs17, masks1, pos_emb0, None, attn_cache, cnn_cache, )
    xs19, _66, new_cnn_cache, = _65
    _67 = torch.slice(xs19, 0, 0, 9223372036854775807, 1)
    _68 = torch.slice(_67, 1, next_cache_start, 9223372036854775807, 1)
    _69 = torch.slice(_68, 2, 0, 9223372036854775807, 1)
    _70 = torch.append(r_elayers_output_cache, _69)
    _71 = torch.append(r_conformer_cnn_cache, new_cnn_cache)
    _72 = torch.__is__(elayers_output_cache0, None)
    if _72:
      attn_cache0, elayers_output_cache2 = None, elayers_output_cache0
    else:
      elayers_output_cache3 = unchecked_cast(List[Tensor], elayers_output_cache0)
      attn_cache0, elayers_output_cache2 = elayers_output_cache3[1], elayers_output_cache3
    _73 = torch.__is__(conformer_cnn_cache0, None)
    if _73:
      cnn_cache0, conformer_cnn_cache2 = None, conformer_cnn_cache0
    else:
      conformer_cnn_cache3 = unchecked_cast(List[Tensor], conformer_cnn_cache0)
      cnn_cache0, conformer_cnn_cache2 = conformer_cnn_cache3[1], conformer_cnn_cache3
    _74 = (_52).forward(xs19, masks1, pos_emb0, None, attn_cache0, cnn_cache0, )
    xs20, _75, new_cnn_cache0, = _74
    _76 = torch.slice(xs20, 0, 0, 9223372036854775807, 1)
    _77 = torch.slice(_76, 1, next_cache_start, 9223372036854775807, 1)
    _78 = torch.slice(_77, 2, 0, 9223372036854775807, 1)
    _79 = torch.append(r_elayers_output_cache, _78)
    _80 = torch.append(r_conformer_cnn_cache, new_cnn_cache0)
    _81 = torch.__is__(elayers_output_cache2, None)
    if _81:
      attn_cache1, elayers_output_cache4 = None, elayers_output_cache2
    else:
      elayers_output_cache5 = unchecked_cast(List[Tensor], elayers_output_cache2)
      attn_cache1, elayers_output_cache4 = elayers_output_cache5[2], elayers_output_cache5
    _82 = torch.__is__(conformer_cnn_cache2, None)
    if _82:
      cnn_cache1, conformer_cnn_cache4 = None, conformer_cnn_cache2
    else:
      conformer_cnn_cache5 = unchecked_cast(List[Tensor], conformer_cnn_cache2)
      cnn_cache1, conformer_cnn_cache4 = conformer_cnn_cache5[2], conformer_cnn_cache5
    _83 = (_53).forward(xs20, masks1, pos_emb0, None, attn_cache1, cnn_cache1, )
    xs21, _84, new_cnn_cache1, = _83
    _85 = torch.slice(xs21, 0, 0, 9223372036854775807, 1)
    _86 = torch.slice(_85, 1, next_cache_start, 9223372036854775807, 1)
    _87 = torch.slice(_86, 2, 0, 9223372036854775807, 1)
    _88 = torch.append(r_elayers_output_cache, _87)
    _89 = torch.append(r_conformer_cnn_cache, new_cnn_cache1)
    _90 = torch.__is__(elayers_output_cache4, None)
    if _90:
      attn_cache2, elayers_output_cache6 = None, elayers_output_cache4
    else:
      elayers_output_cache7 = unchecked_cast(List[Tensor], elayers_output_cache4)
      attn_cache2, elayers_output_cache6 = elayers_output_cache7[3], elayers_output_cache7
    _91 = torch.__is__(conformer_cnn_cache4, None)
    if _91:
      cnn_cache2, conformer_cnn_cache6 = None, conformer_cnn_cache4
    else:
      conformer_cnn_cache7 = unchecked_cast(List[Tensor], conformer_cnn_cache4)
      cnn_cache2, conformer_cnn_cache6 = conformer_cnn_cache7[3], conformer_cnn_cache7
    _92 = (_54).forward(xs21, masks1, pos_emb0, None, attn_cache2, cnn_cache2, )
    xs22, _93, new_cnn_cache2, = _92
    _94 = torch.slice(xs22, 0, 0, 9223372036854775807, 1)
    _95 = torch.slice(_94, 1, next_cache_start, 9223372036854775807, 1)
    _96 = torch.slice(_95, 2, 0, 9223372036854775807, 1)
    _97 = torch.append(r_elayers_output_cache, _96)
    _98 = torch.append(r_conformer_cnn_cache, new_cnn_cache2)
    _99 = torch.__is__(elayers_output_cache6, None)
    if _99:
      attn_cache3, elayers_output_cache8 = None, elayers_output_cache6
    else:
      elayers_output_cache9 = unchecked_cast(List[Tensor], elayers_output_cache6)
      attn_cache3, elayers_output_cache8 = elayers_output_cache9[4], elayers_output_cache9
    _100 = torch.__is__(conformer_cnn_cache6, None)
    if _100:
      cnn_cache3, conformer_cnn_cache8 = None, conformer_cnn_cache6
    else:
      conformer_cnn_cache9 = unchecked_cast(List[Tensor], conformer_cnn_cache6)
      cnn_cache3, conformer_cnn_cache8 = conformer_cnn_cache9[4], conformer_cnn_cache9
    _101 = (_55).forward(xs22, masks1, pos_emb0, None, attn_cache3, cnn_cache3, )
    xs23, _102, new_cnn_cache3, = _101
    _103 = torch.slice(xs23, 0, 0, 9223372036854775807, 1)
    _104 = torch.slice(_103, 1, next_cache_start, 9223372036854775807, 1)
    _105 = torch.slice(_104, 2, 0, 9223372036854775807, 1)
    _106 = torch.append(r_elayers_output_cache, _105)
    _107 = torch.append(r_conformer_cnn_cache, new_cnn_cache3)
    _108 = torch.__is__(elayers_output_cache8, None)
    if _108:
      attn_cache4, elayers_output_cache10 = None, elayers_output_cache8
    else:
      elayers_output_cache11 = unchecked_cast(List[Tensor], elayers_output_cache8)
      attn_cache4, elayers_output_cache10 = elayers_output_cache11[5], elayers_output_cache11
    _109 = torch.__is__(conformer_cnn_cache8, None)
    if _109:
      cnn_cache4, conformer_cnn_cache10 = None, conformer_cnn_cache8
    else:
      conformer_cnn_cache11 = unchecked_cast(List[Tensor], conformer_cnn_cache8)
      cnn_cache4, conformer_cnn_cache10 = conformer_cnn_cache11[5], conformer_cnn_cache11
    _110 = (_56).forward(xs23, masks1, pos_emb0, None, attn_cache4, cnn_cache4, )
    xs24, _111, new_cnn_cache4, = _110
    _112 = torch.slice(xs24, 0, 0, 9223372036854775807, 1)
    _113 = torch.slice(_112, 1, next_cache_start, 9223372036854775807, 1)
    _114 = torch.slice(_113, 2, 0, 9223372036854775807, 1)
    _115 = torch.append(r_elayers_output_cache, _114)
    _116 = torch.append(r_conformer_cnn_cache, new_cnn_cache4)
    _117 = torch.__is__(elayers_output_cache10, None)
    if _117:
      attn_cache5, elayers_output_cache12 = None, elayers_output_cache10
    else:
      elayers_output_cache13 = unchecked_cast(List[Tensor], elayers_output_cache10)
      attn_cache5, elayers_output_cache12 = elayers_output_cache13[6], elayers_output_cache13
    _118 = torch.__is__(conformer_cnn_cache10, None)
    if _118:
      cnn_cache5, conformer_cnn_cache12 = None, conformer_cnn_cache10
    else:
      conformer_cnn_cache13 = unchecked_cast(List[Tensor], conformer_cnn_cache10)
      cnn_cache5, conformer_cnn_cache12 = conformer_cnn_cache13[6], conformer_cnn_cache13
    _119 = (_57).forward(xs24, masks1, pos_emb0, None, attn_cache5, cnn_cache5, )
    xs25, _120, new_cnn_cache5, = _119
    _121 = torch.slice(xs25, 0, 0, 9223372036854775807, 1)
    _122 = torch.slice(_121, 1, next_cache_start, 9223372036854775807, 1)
    _123 = torch.slice(_122, 2, 0, 9223372036854775807, 1)
    _124 = torch.append(r_elayers_output_cache, _123)
    _125 = torch.append(r_conformer_cnn_cache, new_cnn_cache5)
    _126 = torch.__is__(elayers_output_cache12, None)
    if _126:
      attn_cache6, elayers_output_cache14 = None, elayers_output_cache12
    else:
      elayers_output_cache15 = unchecked_cast(List[Tensor], elayers_output_cache12)
      attn_cache6, elayers_output_cache14 = elayers_output_cache15[7], elayers_output_cache15
    _127 = torch.__is__(conformer_cnn_cache12, None)
    if _127:
      cnn_cache6, conformer_cnn_cache14 = None, conformer_cnn_cache12
    else:
      conformer_cnn_cache15 = unchecked_cast(List[Tensor], conformer_cnn_cache12)
      cnn_cache6, conformer_cnn_cache14 = conformer_cnn_cache15[7], conformer_cnn_cache15
    _128 = (_58).forward(xs25, masks1, pos_emb0, None, attn_cache6, cnn_cache6, )
    xs26, _129, new_cnn_cache6, = _128
    _130 = torch.slice(xs26, 0, 0, 9223372036854775807, 1)
    _131 = torch.slice(_130, 1, next_cache_start, 9223372036854775807, 1)
    _132 = torch.slice(_131, 2, 0, 9223372036854775807, 1)
    _133 = torch.append(r_elayers_output_cache, _132)
    _134 = torch.append(r_conformer_cnn_cache, new_cnn_cache6)
    _135 = torch.__is__(elayers_output_cache14, None)
    if _135:
      attn_cache7, elayers_output_cache16 = None, elayers_output_cache14
    else:
      elayers_output_cache17 = unchecked_cast(List[Tensor], elayers_output_cache14)
      attn_cache7, elayers_output_cache16 = elayers_output_cache17[8], elayers_output_cache17
    _136 = torch.__is__(conformer_cnn_cache14, None)
    if _136:
      cnn_cache7, conformer_cnn_cache16 = None, conformer_cnn_cache14
    else:
      conformer_cnn_cache17 = unchecked_cast(List[Tensor], conformer_cnn_cache14)
      cnn_cache7, conformer_cnn_cache16 = conformer_cnn_cache17[8], conformer_cnn_cache17
    _137 = (_59).forward(xs26, masks1, pos_emb0, None, attn_cache7, cnn_cache7, )
    xs27, _138, new_cnn_cache7, = _137
    _139 = torch.slice(xs27, 0, 0, 9223372036854775807, 1)
    _140 = torch.slice(_139, 1, next_cache_start, 9223372036854775807, 1)
    _141 = torch.slice(_140, 2, 0, 9223372036854775807, 1)
    _142 = torch.append(r_elayers_output_cache, _141)
    _143 = torch.append(r_conformer_cnn_cache, new_cnn_cache7)
    _144 = torch.__is__(elayers_output_cache16, None)
    if _144:
      attn_cache8, elayers_output_cache18 = None, elayers_output_cache16
    else:
      elayers_output_cache19 = unchecked_cast(List[Tensor], elayers_output_cache16)
      attn_cache8, elayers_output_cache18 = elayers_output_cache19[9], elayers_output_cache19
    _145 = torch.__is__(conformer_cnn_cache16, None)
    if _145:
      cnn_cache8, conformer_cnn_cache18 = None, conformer_cnn_cache16
    else:
      conformer_cnn_cache19 = unchecked_cast(List[Tensor], conformer_cnn_cache16)
      cnn_cache8, conformer_cnn_cache18 = conformer_cnn_cache19[9], conformer_cnn_cache19
    _146 = (_60).forward(xs27, masks1, pos_emb0, None, attn_cache8, cnn_cache8, )
    xs28, _147, new_cnn_cache8, = _146
    _148 = torch.slice(xs28, 0, 0, 9223372036854775807, 1)
    _149 = torch.slice(_148, 1, next_cache_start, 9223372036854775807, 1)
    _150 = torch.slice(_149, 2, 0, 9223372036854775807, 1)
    _151 = torch.append(r_elayers_output_cache, _150)
    _152 = torch.append(r_conformer_cnn_cache, new_cnn_cache8)
    _153 = torch.__is__(elayers_output_cache18, None)
    if _153:
      attn_cache9, elayers_output_cache20 = None, elayers_output_cache18
    else:
      elayers_output_cache21 = unchecked_cast(List[Tensor], elayers_output_cache18)
      attn_cache9, elayers_output_cache20 = elayers_output_cache21[10], elayers_output_cache21
    _154 = torch.__is__(conformer_cnn_cache18, None)
    if _154:
      cnn_cache9, conformer_cnn_cache20 = None, conformer_cnn_cache18
    else:
      conformer_cnn_cache21 = unchecked_cast(List[Tensor], conformer_cnn_cache18)
      cnn_cache9, conformer_cnn_cache20 = conformer_cnn_cache21[10], conformer_cnn_cache21
    _155 = (_61).forward(xs28, masks1, pos_emb0, None, attn_cache9, cnn_cache9, )
    xs29, _156, new_cnn_cache9, = _155
    _157 = torch.slice(xs29, 0, 0, 9223372036854775807, 1)
    _158 = torch.slice(_157, 1, next_cache_start, 9223372036854775807, 1)
    _159 = torch.slice(_158, 2, 0, 9223372036854775807, 1)
    _160 = torch.append(r_elayers_output_cache, _159)
    _161 = torch.append(r_conformer_cnn_cache, new_cnn_cache9)
    _162 = torch.__is__(elayers_output_cache20, None)
    if _162:
      attn_cache10 = None
    else:
      elayers_output_cache22 = unchecked_cast(List[Tensor], elayers_output_cache20)
      attn_cache10 = elayers_output_cache22[11]
    _163 = torch.__is__(conformer_cnn_cache20, None)
    if _163:
      cnn_cache10 = None
    else:
      conformer_cnn_cache22 = unchecked_cast(List[Tensor], conformer_cnn_cache20)
      cnn_cache10 = conformer_cnn_cache22[11]
    _164 = (_62).forward(xs29, masks1, pos_emb0, None, attn_cache10, cnn_cache10, )
    xs30, _165, new_cnn_cache10, = _164
    _166 = torch.slice(xs30, 0, 0, 9223372036854775807, 1)
    _167 = torch.slice(_166, 1, next_cache_start, 9223372036854775807, 1)
    _168 = torch.slice(_167, 2, 0, 9223372036854775807, 1)
    _169 = torch.append(r_elayers_output_cache, _168)
    _170 = torch.append(r_conformer_cnn_cache, new_cnn_cache10)
    if self.normalize_before:
      xs31 = (self.after_norm).forward(xs30, )
    else:
      xs31 = xs30
    _171 = torch.slice(xs31, 0, 0, 9223372036854775807, 1)
    _172 = torch.slice(_171, 1, cache_size, 9223372036854775807, 1)
    _173 = torch.slice(_172, 2, 0, 9223372036854775807, 1)
    _174 = (_173, r_subsampling_cache, r_elayers_output_cache, r_conformer_cnn_cache)
    return _174
