class ASRModel(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  sos : int
  eos : int
  vocab_size : int
  ignore_id : int
  ctc_weight : float
  reverse_weight : float
  encoder : __torch__.wenet.transformer.encoder.___torch_mangle_21.ConformerEncoder
  decoder : __torch__.wenet.transformer.decoder.___torch_mangle_26.TransformerDecoder
  ctc : __torch__.wenet.transformer.ctc.___torch_mangle_27.CTC
  criterion_att : __torch__.wenet.transformer.label_smoothing_loss.LabelSmoothingLoss
  def forward(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel,
    speech: Tensor,
    speech_lengths: Tensor,
    text: Tensor,
    text_lengths: Tensor) -> Tuple[Optional[Tensor], Optional[Tensor], Optional[Tensor]]:
    _0 = torch.eq(torch.dim(text_lengths), 1)
    if _0:
      pass
    else:
      ops.prim.RaiseException("Exception")
    _1 = torch.eq((torch.size(speech))[0], (torch.size(speech_lengths))[0])
    if _1:
      _3 = torch.eq((torch.size(speech_lengths))[0], (torch.size(text))[0])
      _2 = _3
    else:
      _2 = False
    if _2:
      _5 = torch.eq((torch.size(text))[0], (torch.size(text_lengths))[0])
      _4 = _5
    else:
      _4 = False
    if _4:
      pass
    else:
      ops.prim.RaiseException("Exception")
    _6 = (self.encoder).forward(speech, speech_lengths, 0, -1, )
    encoder_out, encoder_mask, = _6
    encoder_out_lens = torch.sum(torch.squeeze(encoder_mask, 1), [1], False, dtype=None)
    if torch.ne(self.ctc_weight, 1.):
      _7 = (self)._calc_att_loss(encoder_out, encoder_mask, text, text_lengths, )
      loss_att0, acc_att, = _7
      loss_att = loss_att0
    else:
      loss_att = None
    if torch.ne(self.ctc_weight, 0.):
      loss_ctc0 = (self.ctc).forward(encoder_out, encoder_out_lens, text, text_lengths, )
      loss_ctc = loss_ctc0
    else:
      loss_ctc = None
    if torch.__is__(loss_ctc, None):
      loss, loss_att1, loss_ctc1 = loss_att, loss_att, loss_ctc
    else:
      loss_ctc2 = unchecked_cast(Tensor, loss_ctc)
      if torch.__is__(loss_att, None):
        loss0, loss_att2 = loss_ctc2, loss_att
      else:
        loss_att3 = unchecked_cast(Tensor, loss_att)
        _8 = torch.mul(loss_ctc2, self.ctc_weight)
        _9 = torch.mul(loss_att3, torch.sub(1, self.ctc_weight))
        loss0, loss_att2 = torch.add(_8, _9, alpha=1), loss_att3
      loss, loss_att1, loss_ctc1 = loss0, loss_att2, loss_ctc2
    return (loss, loss_att1, loss_ctc1)
  def ctc_activation(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel,
    xs: Tensor) -> Tensor:
    return (self.ctc).log_softmax(xs, )
  def eos_symbol(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel) -> int:
    return self.eos
  def forward_attention_decoder(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel,
    hyps: Tensor,
    hyps_lens: Tensor,
    encoder_out: Tensor,
    reverse_weight: float=0.) -> Tuple[Tensor, Tensor]:
    _10 = __torch__.wenet.utils.common.reverse_pad_list
    _11 = __torch__.wenet.utils.common.add_sos_eos
    _12 = __torch__.torch.nn.functional.log_softmax
    _13 = torch.eq(torch.size(encoder_out, 0), 1)
    if _13:
      pass
    else:
      ops.prim.RaiseException("Exception")
    num_hyps = torch.size(hyps, 0)
    _14 = torch.eq(torch.size(hyps_lens, 0), num_hyps)
    if _14:
      pass
    else:
      ops.prim.RaiseException("Exception")
    encoder_out0 = torch.repeat(encoder_out, [num_hyps, 1, 1])
    _15 = torch.size(encoder_out0, 1)
    _16 = ops.prim.device(encoder_out0)
    encoder_mask = torch.ones([num_hyps, 1, _15], dtype=11, layout=None, device=_16, pin_memory=None)
    r_hyps_lens = torch.sub(hyps_lens, 1, 1)
    _17 = torch.slice(hyps, 0, 0, 9223372036854775807, 1)
    r_hyps = torch.slice(_17, 1, 1, 9223372036854775807, 1)
    r_hyps0 = _10(r_hyps, r_hyps_lens, float(self.ignore_id), )
    _18 = _11(r_hyps0, self.sos, self.eos, self.ignore_id, )
    r_hyps1, _19, = _18
    _20 = (self.decoder).forward(encoder_out0, encoder_mask, hyps, hyps_lens, r_hyps1, reverse_weight, )
    decoder_out, r_decoder_out, _21, = _20
    decoder_out0 = _12(decoder_out, -1, 3, None, )
    r_decoder_out0 = _12(r_decoder_out, -1, 3, None, )
    return (decoder_out0, r_decoder_out0)
  def forward_encoder_chunk(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel,
    xs: Tensor,
    offset: int,
    required_cache_size: int,
    subsampling_cache: Optional[Tensor]=None,
    elayers_output_cache: Optional[List[Tensor]]=None,
    conformer_cnn_cache: Optional[List[Tensor]]=None) -> Tuple[Tensor, Tensor, List[Tensor], List[Tensor]]:
    _22 = (self.encoder).forward_chunk(xs, offset, required_cache_size, subsampling_cache, elayers_output_cache, conformer_cnn_cache, )
    return _22
  def is_bidirectional_decoder(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel) -> bool:
    return False
  def right_context(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel) -> int:
    return self.encoder.embed.right_context
  def sos_symbol(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel) -> int:
    return self.sos
  def subsampling_rate(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel) -> int:
    return self.encoder.embed.subsampling_rate
  def _calc_att_loss(self: __torch__.wenet.transformer.asr_model.___torch_mangle_28.ASRModel,
    encoder_out: Tensor,
    encoder_mask: Tensor,
    ys_pad: Tensor,
    ys_pad_lens: Tensor) -> Tuple[Tensor, float]:
    _23 = __torch__.wenet.utils.common.add_sos_eos
    _24 = __torch__.wenet.utils.common.reverse_pad_list
    _25 = __torch__.wenet.utils.common.th_accuracy
    _26 = _23(ys_pad, self.sos, self.eos, self.ignore_id, )
    ys_in_pad, ys_out_pad, = _26
    ys_in_lens = torch.add(ys_pad_lens, 1, 1)
    r_ys_pad = _24(ys_pad, ys_pad_lens, float(self.ignore_id), )
    _27 = _23(r_ys_pad, self.sos, self.eos, self.ignore_id, )
    r_ys_in_pad, r_ys_out_pad, = _27
    _28 = (self.decoder).forward(encoder_out, encoder_mask, ys_in_pad, ys_in_lens, r_ys_in_pad, self.reverse_weight, )
    decoder_out, r_decoder_out, _29, = _28
    loss_att = (self.criterion_att).forward(decoder_out, ys_out_pad, )
    r_loss_att = torch.tensor(0., dtype=None, device=None, requires_grad=False)
    if torch.gt(self.reverse_weight, 0.):
      r_loss_att1 = (self.criterion_att).forward(r_decoder_out, r_ys_out_pad, )
      r_loss_att0 = r_loss_att1
    else:
      r_loss_att0 = r_loss_att
    _30 = torch.mul(loss_att, torch.sub(1, self.reverse_weight))
    _31 = torch.mul(r_loss_att0, self.reverse_weight)
    loss_att4 = torch.add(_30, _31, alpha=1)
    _32 = torch.view(decoder_out, [-1, self.vocab_size])
    acc_att = _25(_32, ys_out_pad, self.ignore_id, )
    return (loss_att4, acc_att)
